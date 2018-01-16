package co.kukurin;

import static org.junit.Assert.assertEquals;

import co.kukurin.ParameterSupplier.ConstantParameters;
import co.kukurin.ReadHasher.Hash;
import co.kukurin.ReadMapper.CandidateRegion;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class ReadMapperTest {

  @Test
  public void readMapper_regions_collectsMinimumRegionSize() throws Exception {
    // given
    // sketchSize * tau = 5
    ConstantParameters parameters = ConstantParameters.builder()
        .tau(0.5)
        .kmerSize(16)
        .build();
    ReadMapper readMapper = new ReadMapper(new ParameterSupplier(parameters, 10, 10));

    // when
    List<Hash> hashList = Stream.of(1L, 2L).map(Hash::new).collect(Collectors.toList());
    Set<Hash> hashes = new HashSet<>(hashList);

    Map<Hash, Collection<Integer>> hashToReferenceReadIndices = ImmutableMap
        .of(new Hash(1L), Arrays.asList(1, 2, 3),
            new Hash(2L), Arrays.asList(4, 5));

    List<CandidateRegion> regions = readMapper
        .collectCandidateRegions(hashes, hashToReferenceReadIndices);

    // then
    assertEquals(1, regions.size());
    assertEquals(new CandidateRegion(0, 1), regions.get(0));
  }
}
