package co.kukurin;

import co.kukurin.Minimizer.MinimizerValue;
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
                .sketchSize(5)
                .windowSize(90)
                .kmerSize(16)
                .tau(0.1)
                .build();
            Hasher hasher = new Hasher(
                parameterSupplier.getFingerprintingPolynomial(),
                parameterSupplier.getStringToByteArrayConverter(),
                parameterSupplier.getKmerSize());
            Minimizer minimizer = new Minimizer(
                parameterSupplier.getWindowSize());
            Sketcher sketcher = new Sketcher(
                parameterSupplier.getSketchSize());
            ReadMapper readMapper = new ReadMapper(
                parameterSupplier.getSketchSize(),
                parameterSupplier.getTau());

            logMemoryUsage();

            logger.info("Hashing");
            List<MinimizerValue> indexMinimizers = minimizer.minimize(
                hasher.hash(FASTAReader.getInstance(args[0]).readNext().getSequence()));

            logMemoryUsage();

            logger.info("Computing index hash inverse");
            Map<Long, Collection<Integer>> indexHashInverse = inverse(indexMinimizers);

            logger.info("done");
            logMemoryUsage();

            logger.info("Query minimizing");
            List<MinimizerValue> queryMinimizers = minimizer.minimize(
                hasher.hash(FASTAReader.getInstance(args[1]).readNext().getSequence()));

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
    //         int index = indexJaccardPair.getIndex();
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

    private static Map<Long, Collection<Integer>> inverse(List<MinimizerValue> indexHash) {
        Multimap<Long, Integer> result = ArrayListMultimap.create();
        for (int i = 0; i < indexHash.size(); i++) {
            MinimizerValue minimizerValue = indexHash.get(i);
            result.put(
                minimizerValue.getValue().getHash(),
                minimizerValue.getIndex());
        }
        return result.asMap();
    }

}
