package co.kukurin;

import co.kukurin.Hasher.Hash;
import co.kukurin.Minimizer.MinimizerValue;
import co.kukurin.ReadMapper.CandidateRegion;
import co.kukurin.ReadMapper.IndexJaccardPair;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.stream.StreamSupport;
import org.rabinfingerprint.polynomial.Polynomial;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.yeastrc.fasta.FASTAEntry;
import org.yeastrc.fasta.FASTAReader;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Expected usage: [program] [reference FASTA file] [subread]");
            System.exit(1);
        }

        try {
            FASTAReader referenceReader = FASTAReader.getInstance(args[0]);
            List<FASTAEntry> entries = collectEntries(referenceReader);

            FASTAReader subreadReader = FASTAReader.getInstance(args[1]);
            List<FASTAEntry> subreads = collectEntries(subreadReader);

            ParameterSupplier parameterSupplier = ParameterSupplier.builder()
                .fingerprintingPolynomial(Polynomial.createIrreducible(53))
                .stringToByteArrayConverter(String::getBytes)
                .sketchSize(5)
                .windowSize(3)
                .kmerSize(2)
                .tau(0.1)
                .build();

            outputSolution(parameterSupplier, entries, subreads);
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }

    private static void outputSolution(ParameterSupplier parameterSupplier,
        List<FASTAEntry> entries, List<FASTAEntry> subreads) {
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

        for (FASTAEntry entry : entries) {
            for (FASTAEntry subread : subreads) {
               outputSolution(
                   hasher, sketcher, minimizer, readMapper,
                   entry.getSequence(), subread.getSequence());
            }
        }
    }

    private static void outputSolution(
        Hasher hasher,
        Sketcher sketcher,
        Minimizer minimizer,
        ReadMapper readMapper,
        String referenceRead,
        String subread) {
        List<Hash> indexHash = hasher.hash(referenceRead);
        List<Hash> readHash = hasher.hash(subread);

        List<MinimizerValue> minimizerValues = minimizer.minimize(readHash);
        List<MinimizerValue> sketches = sketcher.sketch(minimizerValues);

        List<CandidateRegion> candidateRegions = readMapper.collectCandidateRegions(
            sketches.stream().map(MinimizerValue::getValue).collect(Collectors.toList()),
            inverse(indexHash));

        List<IndexJaccardPair> result = readMapper.collectLikelySimilarRegions(
            indexHash, readHash, candidateRegions);

        result.stream().distinct().forEach(indexJaccardPair -> {
            System.out.println(indexJaccardPair);

            int index = indexJaccardPair.getIndex();
            System.out.println(referenceRead.substring(index, index + subread.length()));
        });
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

    private static Map<Hash, Collection<Integer>> inverse(List<Hash> indexHash) {
        Multimap<Hash, Integer> result = ArrayListMultimap.create();
        for (int i = 0; i < indexHash.size(); i++) {
            Hash hash = indexHash.get(i);
            result.put(hash, i);
        }
        return result.asMap();
    }

}
