package co.kukurin;

import co.kukurin.ReadHasher.Hash;
import co.kukurin.Minimizer.MinimizerValue;
import co.kukurin.ParameterSupplier.ConstantParameters;
import co.kukurin.ReadMapper.CandidateRegion;
import co.kukurin.ReadMapper.IndexJaccardPair;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.rabinfingerprint.polynomial.Polynomial;
import org.yeastrc.fasta.FASTAEntry;
import org.yeastrc.fasta.FASTAReader;

public class Main {

  private static final Logger logger = Logger.getLogger("Main");
  public static final HashFunction HASH_FUNCTION = Hashing.murmur3_128(42);

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      System.err.println("Expected usage: [program] [reference FASTA file] [query FASTA file]");
      System.exit(1);
    }

    try (FileOutputStream fos = new FileOutputStream("out.txt");
         PrintStream out = new PrintStream(fos)) {

      ConstantParameters constantParameters =
          ConstantParameters.builder()
              .fingerprintingPolynomial(Polynomial.createIrreducible(/*degree=*/ 40))
              .stringToByteArrayConverter(String::getBytes)
              // set identical parameters as current impl of MashMap does
              .windowSize(90)
              .kmerSize(16)
              // tau = G(e_max, k) - delta
              // with delta ~0.01
              .tau(0.035)
              .build();

      Minimizer minimizer = new Minimizer(
          constantParameters.getWindowSize(),
          constantParameters.getKmerSize());
      ReadHasher hasher = new ReadHasher(constantParameters.getKmerSize());

      long startTime = System.currentTimeMillis();

      // retain reference minimizers for efficient computation of W(B_i)
      // 4.2. "we store W(B) as an array M of tuples (h, pos)"
      String referenceFilename = args[0];
      List<Hash> referenceHashes = extractHashes(new FastaKmerBufferedReader(
          new FileReader(referenceFilename), constantParameters.getKmerSize()));
      List<MinimizerValue> referenceMinimizers = minimizer.minimize(referenceHashes);

      // "further, to enable O(1) lookup of all the occurences of a particular minimizer's
      // hashed value h, we laso replicate W(B) as a hash table H."
      Map<Hash, Collection<Integer>> inverse = inverse(referenceMinimizers);
      logger.info("Number of hashes total: " + inverse.size());

      String queryFilename = args[1];
      long queryStartTime = System.currentTimeMillis();
      readAllFasta(queryFilename).forEachRemaining(queryEntry -> {
        out.println(queryEntry.getHeaderLine());
        String query = queryEntry.getSequence();

        List<Hash> queryHashes = hasher.hash(query);
        Set<Hash> uniqueHashes = new HashSet<>(queryHashes);

        // 4.3. "to maximize effectiveness of the filter, we set sketch size s = |W_h(A)|
        ParameterSupplier parameterSupplier = new ParameterSupplier(
            constantParameters, query, uniqueHashes.size());
        ReadMapper readMapper =
            new ReadMapper(
                parameterSupplier.getSketchSize(),
                parameterSupplier.getConstantParameters().getTau());

        List<CandidateRegion> candidateRegions =
            readMapper.collectCandidateRegions(uniqueHashes, inverse);
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
      System.err.println("ERROR executing program:");
      System.err.println(e.getLocalizedMessage());
      System.exit(1);
    }
  }

  private static List<Hash> extractHashes(FastaKmerBufferedReader reader) throws IOException {
    List<Hash> hashes = new ArrayList<>();
    for (Iterator<Character> i = reader.nextKmer(); i.hasNext(); i = reader.nextKmer()) {
      com.google.common.hash.Hasher hasher = HASH_FUNCTION.newHasher();
      i.forEachRemaining(hasher::putChar);
      hashes.add(new Hash(hasher.hash().asLong()));
    }
    return hashes;
  }

  private static Iterator<FASTAEntry> readAllFasta(String queryFilename) throws Exception {
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
