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

  private final ParameterSupplier parameterSupplier;

  /**
   * @param queryHashes Hashes obtained from query read.
   * @param hashToReferenceReadIndices map [hash value -> list of indices where the hash is found
   *  in the reference read]
   * @return candidate regions which are estimated to evaluate to desired Jaccard values.
   */
  public List<CandidateRegion> collectCandidateRegions(
      Set<Hash> queryHashes, Map<Hash, Collection<Integer>> hashToReferenceReadIndices) {
    int sketchSize = parameterSupplier.getSketchSize();
    double tau = parameterSupplier.getConstantParameters().getTau();
    int minimumMatching = (int) Math.ceil(sketchSize * tau);
    List<Integer> sortedIndicesInReference =
        queryHashes
            .stream()
            .filter(hashToReferenceReadIndices::containsKey)
            .flatMap(val -> hashToReferenceReadIndices.get(val).stream())
            .sorted()
            .collect(Collectors.toList());
    Stack<CandidateRegion> result = new Stack<>();
    for (int i = 0; i <= sortedIndicesInReference.size() - minimumMatching; i++) {
      int j = i + (minimumMatching - 1);
      int indexHi = sortedIndicesInReference.get(j);
      int indexLo = sortedIndicesInReference.get(i);

      // indexHi and indexLo represent indices in reference read B.
      // size of intersect(A, B) in B from L[i] to L[j] is constant (= minimumMatching).
      // therefore, if range(i, j) is < |A|, Jaccard similarity is expected to be > tau in
      // read B from index position (L[j] - |A|).
      int minDistance = parameterSupplier.getQueryLength();
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
    int sketchSize = parameterSupplier.getSketchSize();
    int countMinimizerWindows = parameterSupplier.getQueryLength()
        - (parameterSupplier.getConstantParameters().getWindowSize() - 1)
        - (parameterSupplier.getConstantParameters().getKmerSize() - 1);

    List<IndexJaccardPair> result = new ArrayList<>();

    for (CandidateRegion candidateRegion : candidateRegions) {
      int i = candidateRegion.getLow();
      int j = i + countMinimizerWindows;

      for (; i <= candidateRegion.getHigh(); i++, j++) {

      }
    }

    return result;
  }

  private List<IndexJaccardPair> newPairs(int i, Set<Hash> hashToAppearanceInBothReads) {
    double jaccardEstimate = solveJaccard(hashToAppearanceInBothReads);
    return jaccardEstimate >= parameterSupplier.getConstantParameters().getTau()
        ? Collections.singletonList(new IndexJaccardPair(i, jaccardEstimate))
        : Collections.emptyList();
  }

  private double solveJaccard(Set<Hash> hashes) {
    // no limit
    int sharedSketch = hashes.size();
    return (1.0 * sharedSketch) / parameterSupplier.getSketchSize();
  }


  // TODO sth here is wrong
  private List<MinimizerValue> getMinimizers(
      List<MinimizerValue> reference, int lowInclusive, int highExclusive) {
    int i = binaryFindIndexOfFirstGteValue(
        reference, MinimizerValue::getOriginalIndex, lowInclusive);
    int j = binaryFindIndexOfFirstGteValue(
        reference, MinimizerValue::getOriginalIndex, highExclusive);
    return reference.subList(i, j);
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
