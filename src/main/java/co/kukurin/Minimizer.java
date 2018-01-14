package co.kukurin;

import co.kukurin.Hasher.Hash;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
public class Minimizer {

  @Value
  public static class MinimizerValue implements Comparable<MinimizerValue> {

    private final int originalIndex;
    private final Hash value;

    @Override
    public int compareTo(MinimizerValue o) {
      int hashCompare = value.compareTo(o.value);

      return hashCompare == 0
          ? Integer.compare(originalIndex, o.originalIndex)
          : hashCompare;
    }
  }

  // private final int sketchSize;
  private final int windowSize;
  private final int kmerSize; // TODO unused (paper algo. computes hash + minimizes at once)

  public List<MinimizerValue> minimize(List<Hash> hashes) {
    Deque<MinimizerValue> deque = new ArrayDeque<>(windowSize);
    List<MinimizerValue> minimizers = new ArrayList<>();

    for (int i = 0; i < hashes.size(); i++) {
      Hash currentHash = hashes.get(i);
      MinimizerValue minimizerValue = new MinimizerValue(i, currentHash);

      // remove elements out of window
      int deletionIndex = i - windowSize;
      while (!deque.isEmpty() && deque.getLast().originalIndex <= deletionIndex) {
        deque.pollLast();
      }

      // compare head to new value
      while (!deque.isEmpty() && deque.getFirst().getValue().compareTo(currentHash) >= 0) {
        deque.pollFirst();
      }

      // push to head
      deque.push(minimizerValue);

      if (i < windowSize - 1) {
        continue;
      }

      if (minimizers.isEmpty() ||
          deque.getLast().getOriginalIndex()
              != minimizers.get(minimizers.size() - 1).getOriginalIndex()) {
        minimizers.add(deque.getLast());
      }
    }

    return minimizers;
  }

}
