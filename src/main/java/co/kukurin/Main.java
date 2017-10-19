package co.kukurin;

import co.kukurin.MappingRead.CandidateRegion;
import co.kukurin.MappingRead.IndexJaccardPair;
import co.kukurin.Minimizer.MinimizerValue;
import co.kukurin.model.Hash;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        String referenceRead = "ATTCGATCGATTACGATTCGATCGATTACGATTCGATCGATTACGATTCGATCGATTACG";
        String subread = "CGATT";

        ParameterSupplier parameterSupplier = new ParameterSupplier(2, 4, 2, 0.15);
        Hasher hasher = new Hasher(parameterSupplier.getKmerSize());
        Minimizer minimizer = new Minimizer(parameterSupplier.getWindowSize());
        Sketcher sketcher = new Sketcher(parameterSupplier.getSketchSize());
        MappingRead mappingRead = new MappingRead();

        List<Hash> indexHash = hasher.hash(referenceRead);
        Map<Hash, Integer> hashToIndexInRead = inverse(indexHash);
        List<Hash> readHash = hasher.hash(subread);

        List<MinimizerValue> minimizerValues = minimizer.minimize(readHash);
        List<MinimizerValue> sketches = sketcher.sketch(minimizerValues);

        List<CandidateRegion> candidateRegions = mappingRead.collectCandidateRanges(
                sketches.stream().map(MinimizerValue::getValue).collect(Collectors.toList()),
                hashToIndexInRead,
                parameterSupplier.getSketchSize(),
                0.02);

        List<IndexJaccardPair> result = mappingRead.collectLikelySimilarRegions(
                indexHash, readHash, candidateRegions, 0.02);

        result.forEach(System.out::println);
    }

    private static Map<Hash, Integer> inverse(List<Hash> indexHash) {
        Map<Hash, Integer> result = new HashMap<>();
        for (int i = 0; i < indexHash.size(); i++) {
            result.put(indexHash.get(i), i);
        }
        return result;
    }

}
