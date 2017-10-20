package co.kukurin;

import co.kukurin.Hasher.Hash;
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
            List<Hash> hashesInIndex,
            List<Hash> hashesInRead,
            List<CandidateRegion> candidateRegions) {
        List<IndexJaccardPair> result = new ArrayList<>();
        Map<Hash, Integer> hashesInReadToZero = hashesInRead.stream().distinct()
                .collect(Collectors.toMap(Function.identity(), ignored -> 0));

        for (CandidateRegion candidateRegion : candidateRegions) {
            int i = candidateRegion.getLow();
            int j = candidateRegion.getHigh();

            Map<Hash, Integer> hashToAppearanceInBothReads = new HashMap<>(hashesInReadToZero);
            getMinimizers(hashesInIndex, i, j).forEach(
                    hash -> hashToAppearanceInBothReads.merge(hash, 0, (k, v) -> 1));
            double jaccard = solveJaccard(hashToAppearanceInBothReads);

            if (jaccard >= tau) {
                result.add(new IndexJaccardPair(i, jaccard));
            }

            for (; i <= candidateRegion.getHigh(); i++, j++) {
                getMinimizers(hashesInIndex, i, i + 1).forEach(
                        hash -> hashToAppearanceInBothReads.compute(hash, (k, v) -> v == 1 ? 0 : null));
                getMinimizers(hashesInIndex, j, j + 1).forEach(
                        hash -> hashToAppearanceInBothReads.merge(hash, 0, (k, v) -> 1));
                jaccard = solveJaccard(hashToAppearanceInBothReads);
                if (jaccard >= tau) {
                    result.add(new IndexJaccardPair(i, jaccard));
                }
            }
        }

        return result;
    }

    private Stream<Hash> getMinimizers(List<Hash> index, int lowInclusive, int highExclusive) {
        return index.stream().skip(lowInclusive).limit(highExclusive - lowInclusive);
    }

    private double solveJaccard(Map<Hash, Integer> hashToAppearance) {
        return 1.0 * hashToAppearance.values().stream().mapToInt(i -> i).sum() / hashToAppearance.size();
    }
}
