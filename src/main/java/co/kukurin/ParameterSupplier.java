package co.kukurin;

import java.util.function.Function;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.rabinfingerprint.polynomial.Polynomial;

@Builder
@Getter
public class ParameterSupplier {

  @Builder
  @Getter
  public static class ConstantParameters {

    private Polynomial fingerprintingPolynomial;
    private Function<String, byte[]> stringToByteArrayConverter;
    private int windowSize;
    private int kmerSize;
    private double tau;
  }

  private final ConstantParameters constantParameters;
  private final int queryLength;
  private final int sketchSize;

  public ParameterSupplier(ConstantParameters constantParameters, String query) {
    this.constantParameters = constantParameters;
    this.queryLength = query.length();
    this.sketchSize = (int) (2.0 * queryLength / constantParameters.windowSize);
  }

}
