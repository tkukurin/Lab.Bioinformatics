package co.kukurin;

import co.kukurin.FastaKmerBufferedReader.KmerSequenceGenerator;
import co.kukurin.Minimizer.MinimizerValue;
import co.kukurin.ParameterSupplier.ConstantParameters;
import co.kukurin.ReadHasher.Hash;
import co.kukurin.ReadMapper.CandidateRegion;
import co.kukurin.ReadMapper.IndexJaccardPair;
import co.kukurin.ReadMapper.ReadMapperResult;
import co.kukurin.benchmarking.Benchmark;
import co.kukurin.benchmarking.PrintStreamBenchmark;
import co.kukurin.benchmarking.TimeBenchmark;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Program entry point.
 */
public class Main {

  private static final Logger logger = Logger.getLogger("Main");
  public static final HashFunction HASH_FUNCTION = Hashing.murmur3_128(42);

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
            // with delta ~0.01
            .tau(0.035)
            .build();

    Benchmark benchmark = new PrintStreamBenchmark(System.out);
    try (FileOutputStream fos = new FileOutputStream(queryPath + "-out.txt");
         PrintStream out = new PrintStream(fos);
         FastaKmerBufferedReader referenceReader = new FastaKmerBufferedReader(
             new FileReader(referenceFilename), constantParameters.getKmerSize());
         FastaKmerBufferedReader queryReader = new FastaKmerBufferedReader(
             new FileReader(queryFilename), constantParameters.getKmerSize())) {

      Minimizer minimizer = new Minimizer(constantParameters.getWindowSize());

      // retain reference minimizers for efficient computation of W(B_i)
      // 4.2. "we store W(B) as an array M of tuples (h, pos)"
      List<Hash> referenceHashes = extractHashes(referenceReader);
      List<MinimizerValue> referenceMinimizers = minimizer.minimize(referenceHashes);

      benchmark.logTime();

      // "further, to enable O(1) lookup of all the occurences of a particular minimizer's
      // hashed value h, we laso replicate W(B) as a hash table H.
      Map<Hash, Collection<Integer>> inverse = inverse(referenceMinimizers);
      logger.info("Number of hashes total: " + inverse.size());

      benchmark.logMemoryUsage();

      TimeBenchmark queryMapBenchmark = new PrintStreamBenchmark(System.out);
      queryMapBenchmark.setStart();

      for (Optional<KmerSequenceGenerator> queryEntryOptional = queryReader.next();
          queryEntryOptional.isPresent();
          queryEntryOptional = queryReader.next()) {
        KmerSequenceGenerator kmerGenerator = queryEntryOptional.get();

        // 4.3. "to maximize effectiveness of the filter, we set sketch size s = |W_h(A)|
        List<Hash> queryHashes = extractHashes(kmerGenerator);
        TreeSet<Hash> uniqueHashes = new TreeSet<>(queryHashes);

        ParameterSupplier parameterSupplier = new ParameterSupplier(
            constantParameters, kmerGenerator.totalReadBytes(), uniqueHashes.size());

        out.println(kmerGenerator.getHeader());

        ReadMapper readMapper = new ReadMapper(parameterSupplier);
        List<CandidateRegion> candidateRegions =
            readMapper.collectCandidateRegions(uniqueHashes, inverse);
        Optional<ReadMapperResult> resultOptional =
            readMapper.findMostLikelyMatch(
                referenceMinimizers, queryHashes, candidateRegions);

        benchmark.logMemoryUsage();
        resultOptional.ifPresent(result -> out.println(
            String.format("%s | %s", result.getIndex(), result.getNucIdentity())));
      }

      queryMapBenchmark.logTime();
    } catch (Exception e) {
      System.out.println("ERROR executing program:");
      System.out.println(e.getLocalizedMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static List<Hash> extractHashes(FastaKmerBufferedReader reader) throws IOException {
    return extractHashes(reader.next().get());
  }

  private static List<Hash> extractHashes(KmerSequenceGenerator kmerSequenceGenerator) throws IOException {
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
