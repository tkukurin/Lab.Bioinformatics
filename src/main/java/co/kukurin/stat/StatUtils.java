package co.kukurin.stat;

import co.kukurin.map.ReadMapper.ReadMapperResult;

/** Utilities for computing various statistic values. */
public class StatUtils {

  private static final double DELTA = 0.015;

  /** Constructs a {@link co.kukurin.map.ReadMapper} result from given values. */
  public static ReadMapperResult toMapperResult(int index, double jaccard, int kmerSize) {
    return new ReadMapperResult(index, jaccard, 1 - jaccardToMash(jaccard, kmerSize));
  }

  /** Estimates error rate from Jaccard values. */
  public static double jaccardToMash(double jaccard, int kmerSize) {
    if(jaccard == 0)
      return 1.0;

    if(jaccard == 1)
      return 0.0;

    return (-1.0 / kmerSize) * Math.log(2.0 * jaccard / (1 + jaccard));
  }

  /** Estimates Jaccard from error rate, with a delta relaxation. */
  public static double mashToJaccardRelaxed(double epsilon, int kmerSize) {
    return mashToJaccard(epsilon, kmerSize) - DELTA;
  }
  /** Estimates Jaccard from error rate. */
  public static double mashToJaccard(double epsilon, int kmerSize) {
    return 1.0 / (2.0 * Math.exp(kmerSize * epsilon) - 1.0);
  }

}
