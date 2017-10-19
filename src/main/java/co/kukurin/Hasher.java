package co.kukurin;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.rabinfingerprint.fingerprint.RabinFingerprintLongWindowed;
import org.rabinfingerprint.polynomial.Polynomial;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@AllArgsConstructor
public class Hasher {

    @Value
    public static class Hash implements Comparable<Hash> {
        private long hash;

        @Override
        public int compareTo(Hash o) {
            return Long.compare(hash, o.getHash());
        }
    }

    private final Polynomial hashingPolynomial;
    private final Function<String, byte[]> stringToByteArrayConverter;
    private final int kmerSize;

    public List<Hash> hash(String read) {
        RabinFingerprintLongWindowed rabinFingerprintLongWindowed =
                new RabinFingerprintLongWindowed(hashingPolynomial, kmerSize);
        byte[] bytesFromString = stringToByteArrayConverter.apply(read);
        List<Hash> result = new ArrayList<>(bytesFromString.length - kmerSize + 1);

        if (kmerSize > read.length()) {
            throw new IllegalArgumentException(
                    "This is probably an error, kmer too large.");
        }

        for (int i = 0; i < kmerSize; i++) {
            rabinFingerprintLongWindowed.pushByte(bytesFromString[i]);
        }

        result.add(new Hash(rabinFingerprintLongWindowed.getFingerprintLong()));
        for (int i = kmerSize; i < read.length(); i++) {
            rabinFingerprintLongWindowed.pushByte(bytesFromString[i]);
            result.add(new Hash(rabinFingerprintLongWindowed.getFingerprintLong()));
        }

        return result;
    }
}
