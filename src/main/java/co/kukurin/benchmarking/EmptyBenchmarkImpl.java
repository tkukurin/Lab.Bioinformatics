package co.kukurin.benchmarking;

/**
 * Dummy benchmark implemntation if no logging is desired.
 */
public class EmptyBenchmarkImpl implements TimeBenchmark, MemoryBenchmark {

  @Override
  public void logMemoryUsage() {

  }

  @Override
  public void setStart() {

  }

  @Override
  public void logTime() {

  }

  // todo

}
