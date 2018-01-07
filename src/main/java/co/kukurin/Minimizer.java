package co.kukurin;

import co.kukurin.Hasher.Hash;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@AllArgsConstructor
public class Minimizer {

    @Value
    public static class MinimizerValue {
        private final int index;
        private final Hash value;
    }

    private final int windowSize;

    public List<MinimizerValue> minimize(List<Hash> hashes) {
        TreeMap<Hash, Integer> hashToLargestIndex = new TreeMap<>();
        List<MinimizerValue> minimizers = new ArrayList<>();

        for (int i = 0; i < windowSize; i++) {
            hashToLargestIndex.put(hashes.get(i), i);
        }

        minimizers.add(extractSmallestMinimizer(hashToLargestIndex));
        for (int i = windowSize; i < hashes.size(); i++) {
            MinimizerValue minimizerValue = extractSmallestMinimizer(hashToLargestIndex);
            if (minimizerValue.getIndex() != minimizers.get(minimizers.size() - 1).getIndex()) {
                minimizers.add(minimizerValue);
            }

            int deletionIndex = i - windowSize;
            hashToLargestIndex.compute(
                    hashes.get(deletionIndex),
                    (key, largestIndex) ->
                            largestIndex == deletionIndex ? null : largestIndex);
            hashToLargestIndex.put(hashes.get(i), i);
        }

        minimizers.add(extractSmallestMinimizer(hashToLargestIndex));

        return minimizers;
    }

    private MinimizerValue extractSmallestMinimizer(TreeMap<Hash, Integer> hashToLargestIndex) {
        Hash smallestValueInWindow = hashToLargestIndex.firstKey();
        int indexForSmallestValue = hashToLargestIndex.get(smallestValueInWindow);
        return new MinimizerValue(indexForSmallestValue, smallestValueInWindow);
    }

}
