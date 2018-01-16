package co.kukurin.benchmarking;

import java.io.PrintStream;
import java.util.function.Supplier;

/**
 * Generic print stream benchmark
 */
public class PrintStreamBenchmark implements Benchmark {

  private final String name;
  private final Supplier<String> logSupplier;

  public PrintStreamBenchmark(String name, Supplier<String> logSupplier) {
    this.name = name;
    this.logSupplier = logSupplier;
  }

  @Override
  public void log(PrintStream output) {
    output.print(String.format("[%s] %s", name, logSupplier.get()));
  }
}
