package co.kukurin.benchmarking;

/**
 * Dummy benchmark implementation if no logging is desired.
 */
public class CompositeDummyBenchmark extends CompositeBenchmark {

  public CompositeDummyBenchmark() {
    super(null);
  }

  @Override
  public void run() {
    this.cancel();
  }
}
