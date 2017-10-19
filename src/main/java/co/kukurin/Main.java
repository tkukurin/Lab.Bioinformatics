package co.kukurin;

import co.kukurin.ReadMapper.CandidateRegion;
import co.kukurin.ReadMapper.IndexJaccardPair;
import co.kukurin.Minimizer.MinimizerValue;
import co.kukurin.model.Hash;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

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
                .sketchSize(5)
                .windowSize(3)
                .kmerSize(2)
                .epsilon(0.15)
                .tau(0.1)
                .build();
        Hasher hasher = new Hasher(parameterSupplier.getKmerSize());
        Minimizer minimizer = new Minimizer(parameterSupplier.getWindowSize());
        Sketcher sketcher = new Sketcher(parameterSupplier.getSketchSize());
        ReadMapper readMapper = new ReadMapper(
                parameterSupplier.getSketchSize(), parameterSupplier.getTau());

        List<Hash> indexHash = hasher.hash(referenceRead);
        Map<Hash, Collection<Integer>> hashToReferenceReadIndices = inverse(indexHash);
        List<Hash> readHash = hasher.hash(subread);

        List<MinimizerValue> minimizerValues = minimizer.minimize(readHash);
        List<MinimizerValue> sketches = sketcher.sketch(minimizerValues);

        List<CandidateRegion> candidateRegions = readMapper.collectCandidateRegions(
                sketches.stream().map(MinimizerValue::getValue).collect(Collectors.toList()),
                hashToReferenceReadIndices);

        List<IndexJaccardPair> result = readMapper.collectLikelySimilarRegions(
                indexHash, readHash, candidateRegions);

        result.stream().distinct().forEach(indexJaccardPair -> {
            System.out.println(indexJaccardPair);

            int index = indexJaccardPair.getIndex();
            System.out.println(referenceRead.substring(index, index + subread.length()));
        });
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
