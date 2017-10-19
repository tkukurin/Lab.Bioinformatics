package co.kukurin;

import co.kukurin.MappingRead.CandidateRegion;
import co.kukurin.MappingRead.IndexJaccardPair;
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

        ParameterSupplier parameterSupplier = new ParameterSupplier(5, 3, 2, 0.15);
        Hasher hasher = new Hasher(parameterSupplier.getKmerSize());
        Minimizer minimizer = new Minimizer(parameterSupplier.getWindowSize());
        Sketcher sketcher = new Sketcher(parameterSupplier.getSketchSize());
        MappingRead mappingRead = new MappingRead();

        List<Hash> indexHash = hasher.hash(referenceRead);
        Map<Hash, Collection<Integer>> hashToIndexInRead = inverse(indexHash);
        List<Hash> readHash = hasher.hash(subread);

        List<MinimizerValue> minimizerValues = minimizer.minimize(readHash);
        List<MinimizerValue> sketches = sketcher.sketch(minimizerValues);

        double tau = 0.1;
        List<CandidateRegion> candidateRegions = mappingRead.collectCandidateRanges(
                sketches.stream().map(MinimizerValue::getValue).collect(Collectors.toList()),
                hashToIndexInRead,
                parameterSupplier.getSketchSize(),
                tau);

        List<IndexJaccardPair> result = mappingRead.collectLikelySimilarRegions(
                indexHash, readHash, candidateRegions, tau);

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
