package co.kukurin;

import lombok.Builder;
import lombok.Getter;
import org.rabinfingerprint.polynomial.Polynomial;

import java.util.function.Function;

@Builder
@Getter
public class ParameterSupplier {

    private final int sketchSize;
    private final int windowSize;
    private final int kmerSize;
    private final double tau;
    private final Polynomial fingerprintingPolynomial;
    private final Function<String, byte[]> stringToByteArrayConverter;

}
