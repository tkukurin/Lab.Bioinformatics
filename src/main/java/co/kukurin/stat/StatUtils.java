package co.kukurin.stat;

import co.kukurin.ReadMapper.ReadMapperResult;

public class StatUtils {

  public static ReadMapperResult toMapperResult(int index, double jaccard, int kmerSize) {
    return new ReadMapperResult(index, jaccard, 1 - jaccardToMash(jaccard, kmerSize));
  }

  public static double jaccardToMash(double jaccard, int kmerSize) {
    if(jaccard == 0)
      return 1.0;

    if(jaccard == 1)
      return 0.0;

    return (-1.0 / kmerSize) * Math.log(2.0 * jaccard / (1 + jaccard));
  }

}
