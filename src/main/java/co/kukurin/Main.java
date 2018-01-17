package co.kukurin;

import co.kukurin.hash.Minimizer;
import co.kukurin.hash.Minimizer.MinimizerValue;
import co.kukurin.ParameterSupplier.ConstantParameters;
import co.kukurin.hash.Hash;
import co.kukurin.map.ReadMapper;
import co.kukurin.map.ReadMapper.CandidateRegion;
import co.kukurin.benchmark.CompositeBenchmark;
import co.kukurin.benchmark.CompositeBenchmarkImpl;
import co.kukurin.benchmark.CompositeDummyBenchmark;
import co.kukurin.benchmark.PrintStreamBenchmark;
import co.kukurin.fasta.FastaKmerBufferedReader;
import co.kukurin.fasta.FastaKmerBufferedReader.KmerSequenceGenerator;
import co.kukurin.stat.StatUtils;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.sun.management.OperatingSystemMXBean;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.management.MBeanServerConnection;

/**
 * Program entry point.
 */
public class Main {

  private static final Logger logger = Logger.getLogger("");
  public static final HashFunction HASH_FUNCTION = Hashing.murmur3_128(/*seed=*/ 42);

  /**
   * @param args Reference and query file in FASTA format. Reference file contains a single read
   * while query file can contain multiple reads.
   */
  public static void main(String[] args) throws IOException {

    if (args.length != 2) {
      System.err.println("Expected parameters: [reference FASTA file] [query FASTA file]");
      System.exit(1);
    }

    String referenceFilename = args[0];
    String queryFilename = args[1];

    String queryPath = Paths.get(queryFilename).getFileName().toString();
    ConstantParameters constantParameters =
        ConstantParameters.builder()
            .windowSize(90)
            .kmerSize(16)
            .tau(StatUtils.mashToJaccard(0.15, 16) - 0.015)
            .build();

    logger.info("Mapping " + queryPath);
    String outputFile = queryPath + "-out.txt";
    String benchmarkFile = queryPath + "-benchmark.txt";
    try (PrintStream out = new PrintStream(new FileOutputStream(outputFile));
         FastaKmerBufferedReader referenceReader = new FastaKmerBufferedReader(
            new FileReader(referenceFilename), constantParameters.getKmerSize());
         FastaKmerBufferedReader queryReader = new FastaKmerBufferedReader(
            new FileReader(queryFilename), constantParameters.getKmerSize())) {

      long startTime = System.currentTimeMillis();
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
        Set<Hash> uniqueHashes = queryHashes.stream().map(MinimizerValue::getHash)
            .collect(Collectors.toSet());

        ParameterSupplier parameterSupplier = new ParameterSupplier(
            constantParameters, kmerGenerator.totalReadBytes(), uniqueHashes.size());

        ReadMapper readMapper = new ReadMapper(parameterSupplier);
        List<CandidateRegion> candidateRegions =
            readMapper.collectCandidateRegions(uniqueHashes, inverse);

        readMapper.findMostLikelyMatch(referenceMinimizers, queryHashes, candidateRegions)
            .ifPresent(result -> {
              out.print("> ");
              out.println(kmerGenerator.getHeader());
              out.println(String.format("position: %s | identity: %s",
                  result.getIndex(),
                  result.getNucIdentity()));
            });
      }

      logger.info("Runtime: " + (System.currentTimeMillis() - startTime) / 1000.0);
    } catch (Exception e) {
      System.out.println("ERROR executing program:");
      System.out.println(e.getLocalizedMessage());

      Files.deleteIfExists(Paths.get(outputFile));
      Files.deleteIfExists(Paths.get(benchmarkFile));

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
    return new CompositeBenchmarkImpl(outStream, memoryBenchmark);
  }

  private static Map<Hash, Collection<Integer>> inverse(List<MinimizerValue> indexHash) {
    Multimap<Hash, Integer> result = ArrayListMultimap.create();
    for (int i = 0; i < indexHash.size(); i++) {
      MinimizerValue minimizerValue = indexHash.get(i);
      result.put(minimizerValue.getHash(), minimizerValue.getOriginalIndex());
    }
    return result.asMap();
  }
}
