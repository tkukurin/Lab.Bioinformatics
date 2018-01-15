package co.kukurin;

import co.kukurin.Minimizer.MinimizerValue;
import co.kukurin.ReadHasher.Hash;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;

/**
 * Performs read mapping of values.
 */
@Value
public class ReadMapper {

  /**
   * Represents a candidate region, i.e. (low, high) indices.
   */
  @Value
  @ToString
  @EqualsAndHashCode
  public static class CandidateRegion {

    private int low;
    private int high;
  }

  /**
   * Tuple consisting of (index of estimate, Jaccard estimate) values.
   */
  @Value
  @ToString
  public static class IndexJaccardPair {

    private int index;
    private double jaccardEstimate;
  }

  private final int sketchSize;
  private final double tau;

  /**
   * @param queryHashes Hashes obtained from query read.
   * @param hashToReferenceReadIndices map [hash value -> list of indices where the hash is found
   *  in the reference read]
   * @return candidate regions which are estimated to evaluate to desired Jaccard values.
   */
  public List<CandidateRegion> collectCandidateRegions(
      Set<Hash> queryHashes, Map<Hash, Collection<Integer>> hashToReferenceReadIndices) {
    int m = (int) Math.ceil(sketchSize * tau);
    List<Integer> sortedIndicesInReference =
        queryHashes
            .stream()
            .filter(hashToReferenceReadIndices::containsKey)
            .flatMap(val -> hashToReferenceReadIndices.get(val).stream())
            .sorted()
            .collect(Collectors.toList());
    Stack<CandidateRegion> result = new Stack<>();
    for (int i = 0; i <= sortedIndicesInReference.size() - m; i++) {
      int j = i + (m - 1);
      int indexHi = sortedIndicesInReference.get(j);
      int indexLo = sortedIndicesInReference.get(i);

      // sketchSize == |A|
      // indexHi and indexLo represent indices in reference read B.
      // size of intersect(A, B) in B from L[i] to L[j] is constant (= m).
      // therefore, if range(i, j) is < |A|, jaccard similarity is expected to be > tau in
      // read B from index position (L[j] - |A|).
      int minDistance = sketchSize;
      if (indexHi - indexLo < minDistance) {
        int low = indexHi - minDistance + 1;

        if (!result.isEmpty() && overlaps(result.peek(), low)) {
          low = result.pop().getLow();
        }

        result.push(new CandidateRegion(Math.max(0, low), indexLo));
      }
    }

    return result;
  }

  private boolean overlaps(CandidateRegion region, int low) {
    return region.getHigh() >= low;
  }

  /**
   * Final step in the mapping, collects similar regions.
   * @param reference Minimizer values collected from reference read.
   * @param hashesInRead Unused?
   * @param candidateRegions Candidate regions obtained from
   *  {@link #collectCandidateRegions(Set, Map)}
   * @return List of (index, Jaccard value) pairs
   */
  public List<IndexJaccardPair> collectLikelySimilarRegions(
      List<MinimizerValue> reference,
      List<Hash> hashesInRead,
      List<CandidateRegion> candidateRegions) {
    List<IndexJaccardPair> result = new ArrayList<>();
    // TODO not sure if I need this anymore?
    // Map<Hash, Integer> hashesInReadToZero =
    //     hashesInRead
    //         .stream()
    //         .distinct()
    //         .collect(Collectors.toMap(Function.identity(), ignored -> 0));

    for (CandidateRegion candidateRegion : candidateRegions) {
      int i = candidateRegion.getLow();
      int j = i + sketchSize;

      // Map<Hash, Integer> hashToAppearanceInBothReads = new HashMap<>(hashesInReadToZero);
      Set<Hash> hashToAppearanceInBothReads = new HashSet<>(getMinimizers(reference, i, j));
          // .forEach(hash -> hashToAppearanceInBothReads.merge(hash, 0, (k, v) -> 1));

      result.addAll(newPairs(i, hashToAppearanceInBothReads));
      for (; i <= candidateRegion.getHigh(); i++, j++) {
        getMinimizers(reference, i, i + 1).forEach(hashToAppearanceInBothReads::remove);
        hashToAppearanceInBothReads.addAll(getMinimizers(reference, j, j + 1));
            // .forEach(hash -> hashToAppearanceInBothReads.merge(hash, 0, (k, v) -> 1));

        result.addAll(newPairs(i, hashToAppearanceInBothReads));
      }
    }

    return result;
  }

  private List<IndexJaccardPair> newPairs(int i, Set<Hash> hashToAppearanceInBothReads) {
    double jaccardEstimate = solveJaccard(hashToAppearanceInBothReads);
    return jaccardEstimate >= tau
        ? Collections.singletonList(new IndexJaccardPair(i, jaccardEstimate))
        : Collections.emptyList();
  }

  private double solveJaccard(Set<Hash> hashes) {
    // no limit
    int sharedSketch = hashes.size();
    return (1.0 * sharedSketch) / sketchSize;
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

  private double solveJaccard(Map<Hash, Integer> hashToAppearance) {
    // no limit
    int sharedSketch = hashToAppearance.values().stream().mapToInt(i -> i).sum();
    return (1.0 * sharedSketch) / sketchSize;
  }

  // TODO sth here is wrong
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
}
