package co.kukurin;

import co.kukurin.Hasher.Hash;
import co.kukurin.Minimizer.MinimizerValue;
import co.kukurin.ReadMapper.CandidateRegion;
import co.kukurin.ReadMapper.IndexJaccardPair;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
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
            System.out.println("Expected usage: [program] [reference FASTA file] [subread]");
            System.exit(1);
        }

        logMemoryUsage();

        try {
            // FASTAReader referenceReader = FASTAReader.getInstance(args[0]);
            // FASTAReader subreadReader = FASTAReader.getInstance(args[1]);

            ParameterSupplier parameterSupplier = ParameterSupplier.builder()
                .fingerprintingPolynomial(Polynomial.createIrreducible(53))
                .stringToByteArrayConverter(String::getBytes)
                .windowSize(90)
                .kmerSize(16)
                .tau(0.1)
                .build();
            Hasher hasher = new Hasher(
                parameterSupplier.getFingerprintingPolynomial(),
                parameterSupplier.getStringToByteArrayConverter(),
                parameterSupplier.getKmerSize());

            logger.info("Query hashing");
            FASTAReader instance = FASTAReader.getInstance(args[1]);
            FASTAEntry entry = instance.readNext();
            String query = null;
            while (!entry.getHeaderLine().contains("17")) {
                entry = instance.readNext();
                query = entry.getSequence();
            }

            System.out.println("Mapping read " + entry.getHeaderLine());

            List<Hash> queryHashes = hasher.hash(query);
            // List<MinimizerValue> queryMinimizers = minimizer.minimize(queryHashes);

            logger.info("Loaded query hashes");
            logMemoryUsage();
            parameterSupplier.setSketchSize((int) (2 * query.length() / 90.0));
            System.out.println("sketch size: " + parameterSupplier.getSketchSize());

                //(int) queryHashes.stream().distinct().count());

            Minimizer minimizer = new Minimizer(
                parameterSupplier.getWindowSize());
            ReadMapper readMapper = new ReadMapper(
                parameterSupplier.getSketchSize(),
                parameterSupplier.getTau());

            logMemoryUsage();

            logger.info("Hashing");
            String sequence = FASTAReader.getInstance(args[0]).readNext().getSequence();
            List<MinimizerValue> referenceMinimizers = minimizer.minimize(hasher.hash(sequence));

            Map<Hash, Collection<Integer>> inverse = inverse(referenceMinimizers);
            logger.info("Number of hashes total: " + inverse.size());
            logger.info("Loading candidate regions");
            List<CandidateRegion> candidateRegions = readMapper.collectCandidateRegions(
                queryHashes, inverse);

            System.out.println("Candidate regions");
            candidateRegions.forEach(System.out::println);

            logger.info("Done");
            logMemoryUsage();

            List<IndexJaccardPair> pairs = readMapper.collectLikelySimilarRegions(
                referenceMinimizers,
                queryHashes,
                candidateRegions);

            logger.info("Found " + pairs.size() + " pairs");
            pairs.forEach(System.out::println);

        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }

    private static void logMemoryUsage() {
        logger.info(String.format("Heap memory usage: %s gb",
            ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1_000_000_000.0));
    }

    // private static void outputSolution(
    //     Hasher hasher,
    //     Sketcher sketcher,
    //     Minimizer minimizer,
    //     ReadMapper readMapper,
    //     String referenceRead,
    //     String subread) {
    //
    //     logger.info("Hashing");
    //     List<Hash> indexHash = hasher.hash(referenceRead);
    //     List<Hash> readHash = hasher.hash(subread);
    //
    //     logger.info("Minimizing");
    //     List<MinimizerValue> minimizerValues = minimizer.minimize(readHash);
    //
    //     logger.info("Extracting candidate regions");
    //     List<CandidateRegion> candidateRegions = readMapper.collectCandidateRegions(
    //         sketcher.sketch(minimizerValues).stream()
    //             .map(MinimizerValue::getValue).collect(Collectors.toList()),
    //         inverse(indexHash));
    //
    //     List<IndexJaccardPair> result = readMapper.collectLikelySimilarRegions(
    //         indexHash, readHash, candidateRegions);
    //
    //     result.stream().distinct().forEach(indexJaccardPair -> {
    //         System.out.println(indexJaccardPair);
    //
    //         int index = indexJaccardPair.getOriginalIndex();
    //         System.out.println(referenceRead.substring(index, index + subread.length()));
    //     });
    // }

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

    private static List<FASTAEntry> collectEntries(FASTAReader referenceReader) throws Exception {
        List<FASTAEntry> entries = new ArrayList<>();
        FASTAEntry entry = referenceReader.readNext();
        while(entry != null) {
          entries.add(entry);
          entry = referenceReader.readNext();
        }
        return entries;
    }

    private static Map<Hash, Collection<Integer>> inverse(List<MinimizerValue> indexHash) {
        Multimap<Hash, Integer> result = ArrayListMultimap.create();
        for (int i = 0; i < indexHash.size(); i++) {
            MinimizerValue minimizerValue = indexHash.get(i);
            result.put(
                minimizerValue.getValue(),
                minimizerValue.getOriginalIndex());
        }
        return result.asMap();
    }

}
