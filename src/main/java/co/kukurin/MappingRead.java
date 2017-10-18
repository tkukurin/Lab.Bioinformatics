package co.kukurin;

import co.kukurin.model.Hash;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Value;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MappingRead {

    @Value
    private class CandidateRegion {
        private int low;
        private int high;

    }

    @Value
    private class IndexJaccardPair {
        private int index;
        private double jaccardSimilarity;
    }

    public List<CandidateRegion> stage1(
            ImmutableList<Hash> readHashes,
            ImmutableMap<Hash, Integer> hashToIndexInReferenceRead,
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

    public List<IndexJaccardPair> stage2(
            ImmutableList<Hash> index,
            ImmutableList<CandidateRegion> candidateRegions,
            double tau) {
        List<CandidateRegion> candidateRegions1 = new LinkedList<>(candidateRegions);
        List<IndexJaccardPair> result = new ArrayList<>();
        Map<Hash, Integer> hashToAppearanceInBothReads = candidateRegions.stream()
                .collect(Collectors.toMap())

        for (CandidateRegion candidateRegion : candidateRegions) {
            int i = candidateRegion.getLow();
            int j = candidateRegion.getHigh();

            candidateRegions1.add(getMinimizers(index, i, j));
            double jaccard = solveJaccard(candidateRegions1);

            if (jaccard >= tau) {
                result.add(new IndexJaccardPair(i, jaccard));
            }

            for (; i < candidateRegion.getHigh(); i++, j++) {
                // TODO delete from candidateregions1
                // TODO insert into candidateregions1
                jaccard = solveJaccard(candidateRegions1);
                if (jaccard >= tau) {
                    result.add(new IndexJaccardPair(i, jaccard));
                }
            }
        }

        return result;
    }

    // TODO some other parameter is necessary.
    private CandidateRegion getMinimizers(ImmutableList<Hash> index, int low, int high) {
        return index.stream().skip(low).limit(high - low + 1).collect();
    }

    private double solveJaccard(ImmutableMap<Hash, Integer> hashToAppearance) {
        return 1.0 * hashToAppearance.values().stream().mapToInt(i -> i).sum() / hashToAppearance.size();
    }
}
