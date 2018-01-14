package co.kukurin;

import co.kukurin.Hasher.Hash;
import co.kukurin.Minimizer.MinimizerValue;
import co.kukurin.ParameterSupplier.ConstantParameters;
import co.kukurin.ReadMapper.CandidateRegion;
import co.kukurin.ReadMapper.IndexJaccardPair;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.rabinfingerprint.polynomial.Polynomial;
import org.yeastrc.fasta.FASTAEntry;
import org.yeastrc.fasta.FASTAReader;

public class Main {

  private static final Logger logger = Logger.getLogger("Main");

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.out.println("Expected usage: [program] [reference FASTA file] [query FASTA file]");
      System.exit(1);
    }

    try (FileOutputStream fos = new FileOutputStream("out.txt");
         PrintStream out = new PrintStream(fos)) {

      ConstantParameters constantParameters =
          ConstantParameters.builder()
              .fingerprintingPolynomial(Polynomial.createIrreducible(/*degree=*/ 53))
              .stringToByteArrayConverter(String::getBytes)
              // set identical parameters as current impl of MashMap does
              .windowSize(90)
              .kmerSize(16)
              // tau = G(e_max, k) - delta
              // with delta ~0.01
              .tau(0.03)
              .build();

      Minimizer minimizer = new Minimizer(
          constantParameters.getWindowSize(),
          constantParameters.getKmerSize());
      Hasher hasher =
          new Hasher(
              constantParameters.getFingerprintingPolynomial(),
              constantParameters.getStringToByteArrayConverter(),
              constantParameters.getKmerSize());

      long startTime = System.currentTimeMillis();

      // retain reference minimizers for efficient computation of W(B_i)
      // 4.2. "we store W(B) as an array M of tuples (h, pos)"
      String referenceFilename = args[0];
      String reference = FASTAReader.getInstance(referenceFilename).readNext().getSequence();
      List<Hash> referenceHashes = hasher.hash(reference);
      List<MinimizerValue> referenceMinimizers = minimizer.minimize(referenceHashes);

      // "further, to enable O(1) lookup of all the occurences of a particular minimizer's
      // hashed value h, we laso replicate W(B) as a hash table H.
      Map<Hash, Collection<Integer>> inverse = inverse(referenceMinimizers);
      logger.info("Number of hashes total: " + inverse.size());

      String queryFilename = args[1];
      long queryStartTime = System.currentTimeMillis();
      streamFastaEntries(queryFilename).forEachRemaining(queryEntry -> {
        String query = queryEntry.getSequence();

        // 4.3. "to maximize effectiveness of the filter, we set sketch size s = |W_h(A)|
        List<Hash> queryHashes = hasher.hash(query);
        TreeSet<Hash> uniqueHashes = new TreeSet<>(queryHashes);

        ParameterSupplier parameterSupplier = new ParameterSupplier(
            constantParameters, query, uniqueHashes.size());

        // logger.info("Mapping read " + queryEntry.getHeaderLine());
        // logger.info("Query length: " + query.length());
        out.println(queryEntry.getHeaderLine());

        // Sketcher sketcher = new Sketcher(parameterSupplier.getSketchSize());
        ReadMapper readMapper =
            new ReadMapper(
                parameterSupplier.getSketchSize(),
                parameterSupplier.getConstantParameters().getTau());

        List<CandidateRegion> candidateRegions =
            readMapper.collectCandidateRegions(queryHashes, inverse);
        List<IndexJaccardPair> pairs =
            readMapper.collectLikelySimilarRegions(
                referenceMinimizers, queryHashes, candidateRegions);

        pairs.stream().map(IndexJaccardPair::toString).forEach(out::println);
      });

      long endTime = System.currentTimeMillis();
      logger.info("Done");
      logger.info("Total time: " + (endTime - startTime) / 1000.0 + "s");
      logger.info("Query map time: " + (endTime - queryStartTime) / 1000.0 + "s");
    } catch (Exception e) {
      System.out.println("ERROR executing program:");
      System.out.println(e.getLocalizedMessage());
      System.exit(1);
    }
  }

  private static void logMemoryUsage() {
    logger.info(
        String.format(
            "Heap memory usage: %s gb",
            ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1_000_000_000.0));
  }

  private static Iterator<FASTAEntry> streamFastaEntries(String queryFilename) throws Exception {
    FASTAReader reader = FASTAReader.getInstance(queryFilename);
    return new Iterator<FASTAEntry>() {
      FASTAEntry current;

      @Override
      public boolean hasNext() {
        try {
          current = reader.readNext();
        } catch (Exception e) {
          current = null;
        }

        return current != null;
      }

      @Override
      public FASTAEntry next() {
        return current;
      }
    };
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
