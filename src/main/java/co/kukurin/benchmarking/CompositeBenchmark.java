package co.kukurin.benchmarking;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;

/**
 * A benchmark base for composite logging.
 */
public abstract class CompositeBenchmark extends TimerTask {

  final PrintStream out;
  final List<Benchmark> benchmarkList;

  public CompositeBenchmark(PrintStream out, PrintStreamBenchmark ... benchmarks) {
    this.benchmarkList = Arrays.asList(benchmarks);
    this.out = out;
  }
}
