package co.kukurin;

import co.kukurin.model.Hash;
import lombok.Value;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MappingRead {

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

    public List<CandidateRegion> collectCandidateRanges(
            List<Hash> readHashes,
            Map<Hash, Integer> hashToIndexInReferenceRead,
            int sketchSize,
            double tau) {

        int m = (int) Math.ceil(sketchSize * tau);
        List<Integer> indicesInReference =
                readHashes.stream().map(hashToIndexInReferenceRead::get).sorted()
                    .collect(Collectors.toList());
        List<CandidateRegion> result = new LinkedList<>();

        for (int i = 0; i < indicesInReference.size() - m; i++) {
            int j = i + (m - 1);
            int indexJ = indicesInReference.get(j);
            int indexI = indicesInReference.get(i);
            if (indexJ - indexI <= readHashes.size()) {
                result.add(new CandidateRegion(indexJ - readHashes.size() + 1, indexI));
            }
        }

        return result;
    }

    public List<IndexJaccardPair> collectLikelySimilarRegions(
            List<Hash> hashesInIndex,
            List<Hash> hashesInRead,
            List<CandidateRegion> candidateRegions,
            double tau) {
        List<IndexJaccardPair> result = new ArrayList<>();
        Map<Hash, Integer> hashToAppearanceInBothReads = hashesInRead.stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> 0));

        for (CandidateRegion candidateRegion : candidateRegions) {
            int i = candidateRegion.getLow();
            int j = candidateRegion.getHigh();

            getMinimizers(hashesInIndex, i, j).forEach(
                    hash -> hashToAppearanceInBothReads.merge(hash, 0, (k, v) -> 1));
            double jaccard = solveJaccard(hashToAppearanceInBothReads);

            if (jaccard >= tau) {
                result.add(new IndexJaccardPair(i, jaccard));
            }

            for (; i < candidateRegion.getHigh(); i++, j++) {
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

    private List<Hash> getMinimizers(List<Hash> index, int lowInclusive, int highExclusive) {
        return index.stream().skip(lowInclusive)
                .limit(highExclusive - lowInclusive)
                .collect(Collectors.toList());
    }

    private double solveJaccard(Map<Hash, Integer> hashToAppearance) {
        return 1.0 * hashToAppearance.values().stream().mapToInt(i -> i).sum() / hashToAppearance.size();
    }
}
