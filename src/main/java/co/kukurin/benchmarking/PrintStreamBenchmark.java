package co.kukurin.benchmarking;

import java.io.PrintStream;

public class PrintStreamBenchmark implements Benchmark {

  private final PrintStream out;
  private long start;

  public PrintStreamBenchmark(PrintStream out) {
    this.out = out;
    this.start = System.currentTimeMillis();
  }

  @Override
  public void logMemoryUsage() {
    long totalBytes = Runtime.getRuntime().totalMemory();
    long freeBytes = Runtime.getRuntime().freeMemory();
    double usedInMb = (totalBytes - freeBytes) / (1_024.0 * 1_024.0);

    logFormat("%.2f Mb", usedInMb);
  }

  @Override
  public void setStart() {
    this.start = System.currentTimeMillis();
  }

  @Override
  public void logTime() {
    long delta = System.currentTimeMillis() - this.start;
    logFormat("%d ms", delta);
  }

  private void logFormat(String format, Object ... message) {
    out.println(String.format(format, message));
  }
}
