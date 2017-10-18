package co.kukurin;

import lombok.AllArgsConstructor;
import lombok.Value;

@AllArgsConstructor
@Value
public class ParameterSupplier {

    private final int sketchSize;
    private final int windowSize;
    private final int kmerSize;
    private final double epsilon;

    public double jaccardEstimate() {
        double denominator = 2 * Math.exp(epsilon * kmerSize) - 1;
        return 1 / denominator;
    }

}
