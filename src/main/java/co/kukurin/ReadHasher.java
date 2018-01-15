package co.kukurin;

import static co.kukurin.Main.HASH_FUNCTION;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.rabinfingerprint.fingerprint.RabinFingerprintLongWindowed;

/**
 * Obtains k-mer hashes for string reads.
 */
@AllArgsConstructor
public class ReadHasher {

  @EqualsAndHashCode
  public static class Hash implements Comparable<Hash> {

    @Getter
    private final long hash;

    Hash(long hash) {
      this.hash = hash;
    }

    Hash(RabinFingerprintLongWindowed rabinFingerprintLongWindowed) {
      this.hash = rabinFingerprintLongWindowed.getFingerprintLong();
    }

    @Override
    public int compareTo(Hash o) {
      return Long.compare(hash, o.getHash());
    }

  }

  private final int kmerSize;

  /**
   * @param read A base pair read
   * @return Hashed values of k-mers (see kmerSize).
   */
  public List<Hash> hash(String read) {
    int length = read.length();

    if (kmerSize > length) {
      throw new IllegalArgumentException(
          "This is probably an error, kmer too large.");
    }

    List<Hash> result = new ArrayList<>();
    for (int i = kmerSize; i <= length; i++) {
      com.google.common.hash.Hasher hasher = HASH_FUNCTION.newHasher();

      for (int j = i - kmerSize; j < i; j++) {
        hasher.putChar(read.charAt(j));
      }

      result.add(new Hash(hasher.hash().asLong()));
    }

    return result;
  }
}
