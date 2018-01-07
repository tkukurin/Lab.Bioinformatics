package co.kukurin;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.rabinfingerprint.polynomial.Polynomial;

import java.util.function.Function;

@Builder
@Setter
@Getter
public class ParameterSupplier {

    private int sketchSize;
    private int windowSize;
    private int kmerSize;
    private double tau;
    private Polynomial fingerprintingPolynomial;
    private Function<String, byte[]> stringToByteArrayConverter;

}
