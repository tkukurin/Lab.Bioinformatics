package co.kukurin;

import com.google.common.io.ByteStreams;
import org.rabinfingerprint.fingerprint.RabinFingerprintLong;
import org.rabinfingerprint.polynomial.Polynomial;

import java.io.FileInputStream;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        // Create new random irreducible polynomial
        // These can also be created from Longs or hex Strings
        Polynomial polynomial = Polynomial.createIrreducible(53);

        // Create a fingerprint object
        RabinFingerprintLong rabin = new RabinFingerprintLong(polynomial);

        // Push bytes from a file stream
        rabin.pushBytes(ByteStreams.toByteArray(new FileInputStream("file.test")));

        // Get fingerprint value and output
        System.out.println(Long.toString(rabin.getFingerprintLong(), 16));
    }

}
