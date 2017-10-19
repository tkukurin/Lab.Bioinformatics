package co.kukurin;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ParameterSupplier {

    private final int sketchSize;
    private final int windowSize;
    private final int kmerSize;
    private final double epsilon;
    private final double tau;

    public double jaccardEstimate() {
        double denominator = 2 * Math.exp(epsilon * kmerSize) - 1;
        return 1 / denominator;
    }

}
