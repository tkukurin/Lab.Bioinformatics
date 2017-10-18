package co.kukurin;

import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

@AllArgsConstructor
public class Minimizer {

    @Value
    public static class MinimizerValue {
        private final int index;
        private final long value;
    }

    private final int windowSize;

    public List<MinimizerValue> minimize(List<Long> hashes) {
        TreeMap<Long, Integer> valueToLargestIndex = new TreeMap<>();
        List<MinimizerValue> minimizers = new ArrayList<>();

        for (int i = 0; i < windowSize; i++) {
            long current = hashes.get(i);
            valueToLargestIndex.put(current, i);
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

    private MinimizerValue extractSmallestMinimizer(TreeMap<Long, Integer> valueToLargestIndex) {
        long smallestValueInWindow = valueToLargestIndex.firstKey();
        int indexForSmallestValue = valueToLargestIndex.get(smallestValueInWindow);
        return new MinimizerValue(indexForSmallestValue, smallestValueInWindow);
    }

    public static void main(String[] args) {
        Minimizer minimizer = new Minimizer(2);
        List<MinimizerValue> minimizerValues = minimizer.minimize(Arrays.asList(2L, 2L, 3L, 8L));
        minimizerValues.forEach(System.out::println);
    }

}
