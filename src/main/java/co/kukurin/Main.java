package co.kukurin;

import co.kukurin.Minimizer.MinimizerValue;
import co.kukurin.ParameterSupplier.ConstantParameters;
import co.kukurin.ReadHasher.Hash;
import co.kukurin.ReadMapper.CandidateRegion;
import co.kukurin.benchmarking.CompositeBenchmark;
import co.kukurin.benchmarking.CompositeBenchmarkImpl;
import co.kukurin.benchmarking.CompositeDummyBenchmark;
import co.kukurin.benchmarking.PrintStreamBenchmark;
import co.kukurin.fasta.FastaKmerBufferedReader;
import co.kukurin.fasta.FastaKmerBufferedReader.KmerSequenceGenerator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.sun.management.OperatingSystemMXBean;
import java.awt.image.ImagingOpException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.management.MBeanServerConnection;

/**
 * Program entry point.
 */
public class Main {

  static final HashFunction HASH_FUNCTION = Hashing.murmur3_128(/*seed=*/ 42);

  /**
   * @param args Reference and query file in FASTA format. Reference file contains a single read
   * while query file can contain multiple reads.
   */
  public static void main(String[] args) throws IOException {

    if (args.length != 2) {
      System.out.println("Expected parameters: [reference FASTA file] [query FASTA file]");
      System.exit(1);
    }

    String referenceFilename = args[0];
    String queryFilename = args[1];

    String queryPath = Paths.get(queryFilename).getFileName().toString();
    System.out.println(queryPath);
    ConstantParameters constantParameters =
        ConstantParameters.builder()
            // set identical parameters as current impl of MashMap does
            .windowSize(90)
            .kmerSize(16)
            // tau = G(e_max, k) - delta
            // e_max = 0.15
            // delta ~= 0.01
            .tau(0.035)
            .build();

    try (PrintStream out = new PrintStream(new FileOutputStream(queryPath + "-out.txt"));
         FastaKmerBufferedReader referenceReader = new FastaKmerBufferedReader(
            new FileReader(referenceFilename), constantParameters.getKmerSize());
         FastaKmerBufferedReader queryReader = new FastaKmerBufferedReader(
            new FileReader(queryFilename), constantParameters.getKmerSize());
         PrintStream benchmarkOut = new PrintStream(new FileOutputStream(queryPath + "-benchmark.txt"))) {

      long startTime = System.currentTimeMillis();
      PrintStreamBenchmark timeBenchmark = new PrintStreamBenchmark(
          "Runtime",
          () -> String.format("%.2f s", (System.currentTimeMillis() - startTime) / 1000.0));
      Timer benchmarkTimer = new Timer("BenchmarkTimer", true);
      benchmarkTimer.scheduleAtFixedRate(
          getBenchmarks(benchmarkOut), TimeUnit.SECONDS.toMillis(0), TimeUnit.SECONDS.toMillis(1));

      Minimizer minimizer = new Minimizer(constantParameters.getWindowSize());

      // retain reference minimizers for efficient computation of W(B_i)
      // 4.2. "we store W(B) as an array M of tuples (h, pos)"
      KmerSequenceGenerator referenceSequenceGenerator = referenceReader.next()
          .orElseThrow(() -> new IOException("Invalid FASTA file " + referenceFilename));
      List<MinimizerValue> referenceMinimizers = minimizer.minimize(referenceSequenceGenerator);

      // "further, to enable O(1) lookup of all the occurences of a particular minimizer's
      // hashed value h, we laso replicate W(B) as a hash table H.
      Map<Hash, Collection<Integer>> inverse = inverse(referenceMinimizers);

      for (Optional<KmerSequenceGenerator> queryEntryOptional = queryReader.next();
          queryEntryOptional.isPresent();
          queryEntryOptional = queryReader.next()) {
        KmerSequenceGenerator kmerGenerator = queryEntryOptional.get();

        // 4.3. "to maximize effectiveness of the filter, we set sketch size s = |W_h(A)|
        List<MinimizerValue> queryHashes = minimizer.minimize(kmerGenerator);
        Set<Hash> uniqueHashes = queryHashes.stream().map(MinimizerValue::getValue)
            .collect(Collectors.toSet());

        ParameterSupplier parameterSupplier = new ParameterSupplier(
            constantParameters, kmerGenerator.totalReadBytes(), uniqueHashes.size());

        ReadMapper readMapper = new ReadMapper(parameterSupplier);
        List<CandidateRegion> candidateRegions =
            readMapper.collectCandidateRegions(uniqueHashes, inverse);

        readMapper.findMostLikelyMatch(referenceMinimizers, queryHashes, null, candidateRegions)
            .ifPresent(result -> {
              out.print(kmerGenerator.getHeader());
              out.println(String.format(" %s | %s", result.getIndex(), result.getNucIdentity()));
            });
      }

      timeBenchmark.log(System.err);
    } catch (Exception e) {
      System.out.println("ERROR executing program:");
      System.out.println(e.getLocalizedMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static CompositeBenchmark getBenchmarks(PrintStream outStream)
      throws IOException {
    if (outStream == null) {
      return new CompositeDummyBenchmark();
    }

    Runtime runtime = Runtime.getRuntime();
    PrintStreamBenchmark memoryBenchmark = new PrintStreamBenchmark("Mem", () -> {
      long usedInBytes = runtime.totalMemory() - runtime.freeMemory();
      return String.format("%.2f Mb", usedInBytes / (1_024.0 * 1_024.0));
    });

    MBeanServerConnection mbsc = ManagementFactory.getPlatformMBeanServer();
    OperatingSystemMXBean osMBean = ManagementFactory.newPlatformMXBeanProxy(
        mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
    PrintStreamBenchmark cpuBenchmark = new PrintStreamBenchmark("Cpu", () ->
        String.format("%.2f", osMBean.getProcessCpuLoad()));

    return new CompositeBenchmarkImpl(outStream, cpuBenchmark, memoryBenchmark);
  }

  private static List<Hash> extractHashes(FastaKmerBufferedReader reader) throws IOException {
    return extractHashes(reader.next().get());
  }

  private static List<Hash> extractHashes(KmerSequenceGenerator kmerSequenceGenerator)
      throws IOException {
    List<Hash> hashes = new ArrayList<>();
    for (Iterator<Character> iterator = kmerSequenceGenerator.readNext();
        iterator.hasNext(); iterator = kmerSequenceGenerator.readNext()) {
      Hasher hasher = HASH_FUNCTION.newHasher();
      iterator.forEachRemaining(hasher::putChar);
      hashes.add(new Hash(hasher.hash().asLong()));
    }
    return hashes;
  }

  private static Map<Hash, Collection<Integer>> inverse(List<MinimizerValue> indexHash) {
    Multimap<Hash, Integer> result = ArrayListMultimap.create();
    for (int i = 0; i < indexHash.size(); i++) {
      MinimizerValue minimizerValue = indexHash.get(i);
      result.put(minimizerValue.getValue(), minimizerValue.getOriginalIndex());
    }
    return result.asMap();
  }
}
