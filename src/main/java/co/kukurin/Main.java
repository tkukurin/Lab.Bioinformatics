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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
              .windowSize(90)
              .kmerSize(16)
              .tau(0.035)
              .build();

      Minimizer minimizer = new Minimizer(constantParameters.getWindowSize());
      Hasher hasher =
          new Hasher(
              constantParameters.getFingerprintingPolynomial(),
              constantParameters.getStringToByteArrayConverter(),
              constantParameters.getKmerSize());

      String referenceFilename = args[0];
      String reference = FASTAReader.getInstance(referenceFilename).readNext().getSequence();
      List<MinimizerValue> referenceMinimizers = minimizer.minimize(hasher.hash(reference));

      Map<Hash, Collection<Integer>> inverse = inverse(referenceMinimizers);
      logger.info("Number of hashes total: " + inverse.size());

      String queryFilename = args[1];
      FASTAReader queryReader = FASTAReader.getInstance(queryFilename);

      FASTAEntry queryEntry;
      while ((queryEntry = queryReader.readNext()) != null) {
        String query = queryEntry.getSequence();

        logger.info("Mapping read " + queryEntry.getHeaderLine());
        logger.info("Query length: " + query.length());
        out.println(queryEntry.getHeaderLine());

        List<Hash> queryHashes = hasher.hash(query);
        ParameterSupplier parameterSupplier = new ParameterSupplier(constantParameters, query);
        ReadMapper readMapper =
            new ReadMapper(parameterSupplier.getSketchSize(),
                parameterSupplier.getConstantParameters().getTau());

        List<CandidateRegion> candidateRegions =
            readMapper.collectCandidateRegions(queryHashes, inverse);
        List<IndexJaccardPair> pairs =
            readMapper.collectLikelySimilarRegions(
                referenceMinimizers, queryHashes, candidateRegions);

        pairs.stream().map(IndexJaccardPair::toString).forEach(out::println);
      }

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

  private static Iterator<FASTAEntry> streamEntries(FASTAReader reader) {
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
