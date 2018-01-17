package co.kukurin;

import lombok.Builder;
import lombok.Getter;

/** Container for algorithm parameter values. */
@Getter
public class ParameterSupplier {

  /** Container for algorithm parameters unaffected by query details. */
  @Builder
  @Getter
  static class ConstantParameters {

    private int windowSize;
    private int kmerSize;
    private double tau;
  }

  private final ConstantParameters constantParameters;
  private final int queryLength;
  private final int sketchSize;

  ParameterSupplier(ConstantParameters constantParameters, int queryLength, int sketchSize) {
    this.constantParameters = constantParameters;
    this.queryLength = queryLength;
    this.sketchSize = sketchSize;
  }

}
