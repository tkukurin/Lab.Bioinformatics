package co.kukurin;

import lombok.Builder;
import lombok.Getter;
import org.rabinfingerprint.polynomial.Polynomial;

@Builder
@Getter
public class ParameterSupplier {

    private final int sketchSize;
    private final int windowSize;
    private final int kmerSize;
    private final double epsilon;
    private final double tau;
    private final Polynomial polynomial;
    
}
