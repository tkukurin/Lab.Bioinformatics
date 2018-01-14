package co.kukurin;

import co.kukurin.Hasher.Hash;
import co.kukurin.Minimizer.MinimizerValue;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import lombok.ToString;
import lombok.Value;

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

      result.addAll(newPairs(i, hashToAppearanceInBothReads));
      for (; i <= candidateRegion.getHigh(); i++, j++) {
        getMinimizers(reference, i, i + 1).forEach(hashToAppearanceInBothReads::remove);
        getMinimizers(reference, j, j + 1)
            .forEach(hash -> hashToAppearanceInBothReads.merge(hash, 0, (k, v) -> 1));

        result.addAll(newPairs(i, hashToAppearanceInBothReads));
      }
    }

    return result;
  }

  /**
   * @return either a singleton or empty list
   */
  private List<IndexJaccardPair> newPairs(int i, Map<Hash, Integer> hashToAppearanceInBothReads) {
    double jaccardEstimate = solveJaccard(hashToAppearanceInBothReads);
    return jaccardEstimate >= tau
        ? Collections.singletonList(new IndexJaccardPair(i, jaccardEstimate))
        : Collections.emptyList();
  }

  private List<Hash> getMinimizers(
      List<MinimizerValue> reference, int lowInclusive, int highExclusive) {
    int i = binaryFindIndexOfFirstGteValue(
        reference, MinimizerValue::getOriginalIndex, lowInclusive);

    List<Hash> result = new ArrayList<>();
    while (i < reference.size() && reference.get(i).getOriginalIndex() < highExclusive) {
      result.add(reference.get(i++).getValue());
    }
    return result;
  }

  private int binaryFindIndexOfFirstGteValue(
      List<MinimizerValue> index,
      ToIntFunction<MinimizerValue> intComparator,
      int startingKey) {
    int i = Collections.binarySearch(
        index, new MinimizerValue(startingKey, null), Comparator.comparingInt(intComparator));
    return i < 0 ? -i : i;
  }

  private double solveJaccard(Map<Hash, Integer> hashToAppearance) {
    int sharedSketch = hashToAppearance.values().stream().limit(sketchSize).mapToInt(i -> i).sum();
    return 1.0 * sharedSketch / sketchSize;
  }
}
