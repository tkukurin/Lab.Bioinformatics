package co.kukurin;

import co.kukurin.Hasher.Hash;
import co.kukurin.Minimizer.MinimizerValue;
import java.util.function.ToIntFunction;
import java.util.logging.Logger;
import lombok.ToString;
import lombok.Value;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
public class ReadMapper {

  @Value
  @ToString
  public static class CandidateRegion {
    private int low;
    private int high;
  }

  @Value
  @ToString
  public static class IndexJaccardPair {
    private int index;
    private double jaccardEstimate;
  }

  private static final Logger logger = Logger.getLogger("ReadMapper");

  private final int sketchSize;
  private final double tau;

  public List<CandidateRegion> collectCandidateRegions(
      List<Hash> readHashes, Map<Hash, Collection<Integer>> hashToReferenceReadIndices) {
    int m = (int) Math.ceil(sketchSize * tau);
    List<Integer> sortedIndicesInReference =
        readHashes
            .stream()
            .filter(hashToReferenceReadIndices::containsKey)
            .flatMap(val -> hashToReferenceReadIndices.get(val).stream())
            .sorted()
            .collect(Collectors.toList());
    Stack<CandidateRegion> result = new Stack<>();

    logger.info(sortedIndicesInReference.size() + " sorted indices");
    sortedIndicesInReference.stream().limit(4).forEach(System.out::println);

    for (int i = 0; i <= sortedIndicesInReference.size() - m; i++) {
      int j = i + (m - 1);
      int indexJ = sortedIndicesInReference.get(j);
      int indexI = sortedIndicesInReference.get(i);

      // TODO |A|, not hashes?
      // indexJ and indexI represent indices in reference read B.
      // size of intersect(A, B) in [i, j] in B is constant (= m).
      // therefore, if range(i, j) is < |A|, jaccard similarity is expected to be > tau in
      // read B from index position (j - |A|).
      if (indexJ - indexI < readHashes.size()) {
        int low = indexJ - readHashes.size() + 1;

        if (!result.isEmpty() && overlaps(result.peek(), low)) {
          low = result.pop().getLow();
        }

        result.push(new CandidateRegion(Math.max(0, low), indexI));
      }
    }

    return result;
  }

  private boolean overlaps(CandidateRegion region, int low) {
    return region.getHigh() >= low;
  }

  public List<IndexJaccardPair> collectLikelySimilarRegions(
      List<MinimizerValue> reference,
      List<Hash> hashesInRead,
      List<CandidateRegion> candidateRegions) {
    List<IndexJaccardPair> result = new ArrayList<>();
    // TODO not sure if I need this anymore?
    Map<Hash, Integer> hashesInReadToZero =
        hashesInRead
            .stream()
            .distinct()
            .collect(Collectors.toMap(Function.identity(), ignored -> 0));

    for (CandidateRegion candidateRegion : candidateRegions) {
      int i = candidateRegion.getLow();
      // TODO i + |A|
      int j = i + hashesInRead.size();

      Map<Hash, Integer> hashToAppearanceInBothReads = new HashMap<>(hashesInReadToZero);
      getMinimizers(reference, i, j)
          .forEach(hash -> hashToAppearanceInBothReads.merge(hash, 0, (k, v) -> 1));
      double jaccardEstimate = solveJaccard(hashToAppearanceInBothReads);

      logger.info("jaccard estimate: " + jaccardEstimate);

      if (jaccardEstimate >= tau) {
        result.add(new IndexJaccardPair(i, jaccardEstimate));
      }

      for (; i <= candidateRegion.getHigh(); i++, j++) {
        getMinimizers(reference, i, i + 1).forEach(hashToAppearanceInBothReads::remove);
        getMinimizers(reference, j, j + 1)
            .forEach(hash -> hashToAppearanceInBothReads.merge(hash, 0, (k, v) -> 1));
        jaccardEstimate = solveJaccard(hashToAppearanceInBothReads);
        logger.info("jaccard estimate: " + jaccardEstimate);
        if (jaccardEstimate >= tau) {
          result.add(new IndexJaccardPair(i, jaccardEstimate));
        }
      }
    }

    return result;
  }

  private List<Hash> getMinimizers(
      List<MinimizerValue> reference, int lowInclusive, int highExclusive) {
    int i =
        binaryFindIndexOfFirstGteValue(
            reference, MinimizerValue::getOriginalIndex, new MinimizerValue(lowInclusive, null));

    List<Hash> result = new ArrayList<>();
    while (i < reference.size() && reference.get(i).getOriginalIndex() < highExclusive) {
      result.add(reference.get(i++).getValue());
    }
    return result;
  }

  private int binaryFindIndexOfFirstGteValue(
      List<MinimizerValue> index,
      ToIntFunction<MinimizerValue> intComparator,
      MinimizerValue startingKey) {
    int i = Collections.binarySearch(index, startingKey, Comparator.comparingInt(intComparator));
    return i < 0 ? -i : i;
  }

  private double solveJaccard(Map<Hash, Integer> hashToAppearance) {
    int sharedSketch = hashToAppearance.values().stream().limit(sketchSize).mapToInt(i -> i).sum();
    return 1.0 * sharedSketch / sketchSize;
  }
}
