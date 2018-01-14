package co.kukurin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.rabinfingerprint.fingerprint.RabinFingerprintLongWindowed;
import org.rabinfingerprint.polynomial.Polynomial;

@AllArgsConstructor
public class Hasher {

  @EqualsAndHashCode
  public static class Hash implements Comparable<Hash> {

    @Getter
    private long hash;

    Hash(RabinFingerprintLongWindowed rabinFingerprintLongWindowed) {
      this.hash = rabinFingerprintLongWindowed.getFingerprintLong();
    }

    @Override
    public int compareTo(Hash o) {
      return Long.compare(hash, o.getHash());
    }
  }

  private final Polynomial hashingPolynomial;
  private final Function<String, byte[]> stringToByteArrayConverter;
  private final int kmerSize;

  public List<Hash> getUniqueSortedHashes(String read) {
    int length = read.length();

    if (kmerSize > length) {
      throw new IllegalArgumentException("This is probably an error, kmer too large.");
    }

    RabinFingerprintLongWindowed rabinFingerprintLongWindowed =
        new RabinFingerprintLongWindowed(hashingPolynomial, kmerSize);
    byte[] bytesFromString = stringToByteArrayConverter.apply(read);
    Set<Hash> result = new TreeSet<>();

    for (int i = 0; i < kmerSize; i++) {
      rabinFingerprintLongWindowed.pushByte(bytesFromString[i]);
    }

    result.add(new Hash(rabinFingerprintLongWindowed));
    for (int i = kmerSize; i < length; i++) {
      rabinFingerprintLongWindowed.pushByte(bytesFromString[i]);
      result.add(new Hash(rabinFingerprintLongWindowed));
    }

    return new ArrayList<>(result);
  }
}
