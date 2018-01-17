package co.kukurin.hash;

import static co.kukurin.Main.HASH_FUNCTION;

import co.kukurin.fasta.FastaKmerBufferedReader.KmerSequenceGenerator;
import com.google.common.hash.Hasher;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Minimizes a read.
 */
@AllArgsConstructor
public class Minimizer {

  /**
   * Class which contains (Index, Hash) pairs used as minimizer outputs.
   */
  @Value
  @EqualsAndHashCode
  public static class MinimizerValue implements Comparable<MinimizerValue> {

    private final int originalIndex;
    private final Hash hash;

    @Override
    // compares hashes lo - hi, and indices hi - lo
    public int compareTo(MinimizerValue o) {
      int hashCompare = hash.compareTo(o.hash);

      return hashCompare == 0
          ? Integer.compare(o.originalIndex, originalIndex)
          : hashCompare;
    }
  }

  private final int windowSize;

  /**
   * @param  generator A k-mer generator.
   * @return List of minimizer values, whose index distance is at most (windowSize - 1). A minimizer
   * is defined to be a hash with smallest hash or (in case of hash equality) largest index within
   * a window.
   */
  public List<MinimizerValue> minimize(KmerSequenceGenerator generator) throws IOException {
    Deque<MinimizerValue> deque = new ArrayDeque<>(windowSize);
    List<MinimizerValue> minimizers = new ArrayList<>();
    int i = 0;

    for (Iterator<Character> kmerIterator = generator.readNext();
        kmerIterator.hasNext(); kmerIterator = generator.readNext()) {
      MinimizerValue minimizerValue = makeMinimizer(i++, kmerIterator);
      Hash currentHash = minimizerValue.getHash();

      // remove elements out of window
      int deletionIndex = i - windowSize;
      while (!deque.isEmpty() && deque.getLast().originalIndex <= deletionIndex) {
        deque.pollLast();
      }

      // compare head to new hash
      while (!deque.isEmpty() && deque.getFirst().getHash().compareTo(currentHash) >= 0) {
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

  private MinimizerValue makeMinimizer(int i, Iterator<Character> kmerIterator) {
    Hasher hasher = HASH_FUNCTION.newHasher();
    kmerIterator.forEachRemaining(hasher::putChar);
    Hash currentHash = new Hash(hasher.hash().asLong());
    return new MinimizerValue(i, currentHash);
  }

}
