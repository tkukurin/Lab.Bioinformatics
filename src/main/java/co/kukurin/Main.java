package co.kukurin;

import co.kukurin.Minimizer.MinimizerValue;
import co.kukurin.ReadMapper.CandidateRegion;
import co.kukurin.ReadMapper.IndexJaccardPair;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.rabinfingerprint.polynomial.Polynomial;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
//        String referenceRead = "ATTCGATCGATTACGATTCGATCGATTACGATTCGATCGATTACGATTCGATCGATTACG";
//        String subread = "CGATTCCGATTCGAT";
        String referenceRead = "ovo je neki reference dokument iz korpusa";
        String subread = "neki document";

        ParameterSupplier parameterSupplier = ParameterSupplier.builder()
                .fingerprintingPolynomial(Polynomial.createIrreducible(53))
                .stringToByteArrayConverter(String::getBytes)
                .sketchSize(5)
                .windowSize(3)
                .kmerSize(2)
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

        List<Hasher.Hash> indexHash = hasher.hash(referenceRead);
        List<Hasher.Hash> readHash = hasher.hash(subread);

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

    private static Map<Hasher.Hash, Collection<Integer>> inverse(List<Hasher.Hash> indexHash) {
        Multimap<Hasher.Hash, Integer> result = ArrayListMultimap.create();
        for (int i = 0; i < indexHash.size(); i++) {
            Hasher.Hash hash = indexHash.get(i);
            result.put(hash, i);
        }
        return result.asMap();
    }

}
