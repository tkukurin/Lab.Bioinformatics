package co.kukurin;

import co.kukurin.model.Hash;
import lombok.AllArgsConstructor;
import org.rabinfingerprint.fingerprint.RabinFingerprintLongWindowed;
import org.rabinfingerprint.polynomial.Polynomial;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class Hasher {

    private final int kmerSize;

    public List<Hash> hash(String read) {
        Polynomial polynomial = Polynomial.createIrreducible(53);
        RabinFingerprintLongWindowed rabinFingerprintLongWindowed =
                new RabinFingerprintLongWindowed(polynomial, kmerSize);
        List<Hash> result = new ArrayList<>(read.length() - kmerSize + 1);

        if (kmerSize > read.length()) {
            throw new IllegalArgumentException(
                    "This is probably an error, kmer too large.");
        }

        for (int i = 0; i < kmerSize; i++) {
            rabinFingerprintLongWindowed.pushByte((byte) read.charAt(i));
        }

        result.add(new Hash(rabinFingerprintLongWindowed.getFingerprintLong()));
        for (int i = kmerSize; i < read.length(); i++) {
            rabinFingerprintLongWindowed.pushByte((byte) read.charAt(i));
            result.add(new Hash(rabinFingerprintLongWindowed.getFingerprintLong()));
        }

        return result;
    }

}
