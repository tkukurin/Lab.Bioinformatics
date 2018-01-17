package co.kukurin.benchmark;

import java.io.PrintStream;

/**
 * Implementation of a composite benchmark to be used as a timer task.
 */
public class CompositeBenchmarkImpl extends CompositeBenchmark {

  public CompositeBenchmarkImpl(PrintStream out, PrintStreamBenchmark... benchmarks) {
    super(out, benchmarks);
  }

  @Override
  public void run() {
    benchmarkList.forEach(benchmark -> {
      benchmark.log(out);
      out.print(" ");
    });

    out.println();
  }
}
