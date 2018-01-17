package co.kukurin.map;

import co.kukurin.hash.Minimizer.MinimizerValue;
import co.kukurin.ParameterSupplier;
import co.kukurin.hash.Hash;
import co.kukurin.stat.StatUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
   * Output of read mapper.
   */
  @Value
  @ToString
  public static class ReadMapperResult {

    private int index;
    private double jaccardEstimate;
    private double nucIdentity;
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
    int minShared = (int) Math.ceil(sketchSize * tau);
    List<Integer> sortedIndicesInReference =
        queryHashes
            .stream()
            .filter(hashToReferenceReadIndices::containsKey)
            .flatMap(val -> hashToReferenceReadIndices.get(val).stream())
            .sorted()
            .collect(Collectors.toList());
    Stack<CandidateRegion> result = new Stack<>();
    for (int i = 0; i <= sortedIndicesInReference.size() - minShared; i++) {
      int j = i + (minShared - 1);
      int indexHi = sortedIndicesInReference.get(j);
      int indexLo = sortedIndicesInReference.get(i);

      // indexHi and indexLo represent indices in reference read B.
      // size of intersect(A, B) in B from L[i] to L[j] is constant (= minShared).
      // therefore, if range(i, j) is < |A|, jaccard similarity is expected to be > tau in
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
   * Final step in the mapping, finds best match.
   * @param reference Minimizer values collected from reference read (sorted ascending by index).
   * @param query Minimizer values collected from the query (sorted ascending by index).
   * @param candidateRegions Candidate regions obtained from
   *  {@link #collectCandidateRegions(Set, Map)}
   * @return Best estimated match.
   */
  public Optional<ReadMapperResult> findMostLikelyMatch(
      List<MinimizerValue> reference,
      List<MinimizerValue> query,
      List<CandidateRegion> candidateRegions) {
    int index = -1;
    int maxMinimizers = 0;

    for (CandidateRegion candidateRegion : candidateRegions) {
      int windowStart = candidateRegion.getLow();
      int windowEnd = windowStart + parameterSupplier.getQueryLength();

      SketchMap sketchMap = new SketchMap(query);
      int minimizersStart = binaryFindIndexOfFirstGteValue(
          reference, MinimizerValue::getOriginalIndex, windowStart);
      int minimizersEnd = binaryFindIndexOfFirstGteValue(
          reference, MinimizerValue::getOriginalIndex, windowEnd);

      while(windowStart <= candidateRegion.getHigh()) {
        // throw out values leaving window
        if (minimizersStart < reference.size()
            && reference.get(minimizersStart).getOriginalIndex() <= windowStart) {
          sketchMap.removeReference(reference.get(minimizersStart));
          minimizersStart++;
        }

        // insert values entering window
        if (minimizersEnd < reference.size()
            && reference.get(minimizersEnd).getOriginalIndex() <= windowEnd) {
          sketchMap.putReference(reference.get(minimizersEnd));
          minimizersEnd++;
        }

        int sharedMinimizers = sketchMap.getSharedMinimizers(parameterSupplier.getSketchSize());
        if (sharedMinimizers > maxMinimizers) {
          index = windowStart;
          maxMinimizers = sharedMinimizers;
        }

        // skip until first following window with changes
        int skip = reference.get(minimizersStart).getOriginalIndex() - windowStart;
        if (minimizersEnd < reference.size()) {
          skip = Math.min(skip, reference.get(minimizersEnd).getOriginalIndex() - windowEnd + 1);
        }

        windowStart += skip;
        windowEnd += skip;
      }
    }

    int kmerSize = parameterSupplier.getConstantParameters().getKmerSize();
    double jaccard = (1.0 * maxMinimizers) / parameterSupplier.getQueryLength();
    return index == - 1
        ? Optional.empty()
        : Optional.of(StatUtils.toMapperResult(index, jaccard, kmerSize));
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
