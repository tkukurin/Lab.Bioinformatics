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
        TreeMap<Hash, Integer> valueToLargestIndex = new TreeMap<>();
        List<MinimizerValue> minimizers = new ArrayList<>();

        for (int i = 0; i < windowSize; i++) {
            valueToLargestIndex.put(hashes.get(i), i);
        }

        for (int i = windowSize; i < hashes.size(); i++) {
            minimizers.add(extractSmallestMinimizer(valueToLargestIndex));

            int deletionIndex = i - windowSize;
            valueToLargestIndex.compute(
                    hashes.get(deletionIndex),
                    (key, largestIndex) ->
                            largestIndex == deletionIndex ? null : largestIndex);
            valueToLargestIndex.put(hashes.get(i), i);
        }

        minimizers.add(extractSmallestMinimizer(valueToLargestIndex));

        return minimizers;
    }

    private MinimizerValue extractSmallestMinimizer(TreeMap<Hash, Integer> valueToLargestIndex) {
        Hash smallestValueInWindow = valueToLargestIndex.firstKey();
        int indexForSmallestValue = valueToLargestIndex.get(smallestValueInWindow);
        return new MinimizerValue(indexForSmallestValue, smallestValueInWindow);
    }

}
