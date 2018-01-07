package co.kukurin;

import co.kukurin.Hasher.Hash;
import co.kukurin.Minimizer.MinimizerValue;
import java.util.function.Consumer;
import lombok.Value;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value
public class ReadMapper {

    @Value
    public static class CandidateRegion {
        private int low;
        private int high;

    }

    @Value
    public static class IndexJaccardPair {
        private int index;
        private double jaccardSimilarity;
    }

    private final int sketchSize;
    private final double tau;

    public List<CandidateRegion> collectCandidateRegions(
            List<Hash> readHashes,
            Map<Hash, Collection<Integer>> hashToReferenceReadIndices) {
        int m = (int) Math.ceil(sketchSize * tau);
        List<Integer> sortedIndicesInReference =
                readHashes.stream()
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
            List<MinimizerValue> hashesInIndex,
            List<Hash> hashesInRead,
            List<CandidateRegion> candidateRegions) {
        List<IndexJaccardPair> result = new ArrayList<>();
        // TODO not sure if I need this anymore?
        Map<Hash, Integer> hashesInReadToZero = hashesInRead.stream().distinct()
                .collect(Collectors.toMap(Function.identity(), ignored -> 0));

        for (CandidateRegion candidateRegion : candidateRegions) {
            int i = candidateRegion.getLow();
            // TODO i + |A|
            int j = i + hashesInRead.size();

            Map<Hash, Integer> hashToAppearanceInBothReads = new HashMap<>(hashesInReadToZero);
            getMinimizers(hashesInIndex, i, j).forEach(
                    hash -> hashToAppearanceInBothReads.merge(hash, 0, (k, v) -> 1));
            double jaccardEstimate = solveJaccard(hashToAppearanceInBothReads);

            if (jaccardEstimate >= tau) {
                result.add(new IndexJaccardPair(i, jaccardEstimate));
            }

            for (; i <= candidateRegion.getHigh(); i++, j++) {
                getMinimizers(hashesInIndex, i, i + 1).forEach(
                    hashToAppearanceInBothReads::remove);
                getMinimizers(hashesInIndex, j, j + 1).forEach(
                    hash -> hashToAppearanceInBothReads.merge(hash, 0, (k, v) -> 1));
                jaccardEstimate = solveJaccard(hashToAppearanceInBothReads);
                if (jaccardEstimate >= tau) {
                    result.add(new IndexJaccardPair(i, jaccardEstimate));
                }
            }
        }

        return result;
    }

    private Stream<Hash> getMinimizers(List<MinimizerValue> index, int lowInclusive, int highExclusive) {
        return index.stream().skip(lowInclusive).limit(highExclusive - lowInclusive)
            .map(MinimizerValue::getValue);
    }

    private double solveJaccard(Map<Hash, Integer> hashToAppearance) {
        return 1.0 * hashToAppearance.values().stream().mapToInt(i -> i).sum() / sketchSize;
    }
}
