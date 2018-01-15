package co.kukurin;

import static org.junit.Assert.assertEquals;

import co.kukurin.ReadHasher.Hash;
import co.kukurin.Minimizer.MinimizerValue;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class MinimizerTest {

  @Test
  public void minimizer_ordered_shouldKeepWindow() throws Exception {
    // given
    Minimizer minimizer = new Minimizer(3);
    List<Hash> hashes = Stream.of(1, 2, 3, 4, 5, 6).map(Hash::new).collect(Collectors.toList());

    // when
    List<MinimizerValue> minimizerValues = minimizer.minimize(hashes);

    // then
    assertEquals(4, minimizerValues.size());
    assertEquals(1, minimizerValues.get(0).getValue().getHash());
    assertEquals(2, minimizerValues.get(1).getValue().getHash());
    assertEquals(3, minimizerValues.get(2).getValue().getHash());
    assertEquals(4, minimizerValues.get(3).getValue().getHash());
  }

  @Test
  public void minimizer_unordered_shouldKeepValue() throws Exception {
    // given
    Minimizer minimizer = new Minimizer(3);
    List<Hash> hashes = Stream.of(3, 2, 1, 4, 5, 6).map(Hash::new).collect(Collectors.toList());

    // when
    List<MinimizerValue> minimizerValues = minimizer.minimize(hashes);

    // then
    // windows: [ <hash 1, index 2>, <hash 4, index 3> ]
    assertEquals(2, minimizerValues.size());

    assertEquals(1, minimizerValues.get(0).getValue().getHash());
    assertEquals(2, minimizerValues.get(0).getOriginalIndex());

    assertEquals(4, minimizerValues.get(1).getValue().getHash());
    assertEquals(3, minimizerValues.get(1).getOriginalIndex());
  }

  @Test
  public void minimizer_hashesEqual_shouldKeepLargestIndex() throws Exception {
    // given
    Minimizer minimizer = new Minimizer(3);
    List<Hash> hashes = Stream.of(1, 1, 1, 1, 1, 1).map(Hash::new).collect(Collectors.toList());

    // when
    List<MinimizerValue> minimizerValues = minimizer.minimize(hashes);

    // then
    // windows: [ <hash 1, index 2>, <hash 1, index 3>,... ]
    assertEquals(4, minimizerValues.size());

    assertEquals(1, minimizerValues.get(0).getValue().getHash());
    assertEquals(2, minimizerValues.get(0).getOriginalIndex());

    assertEquals(1, minimizerValues.get(1).getValue().getHash());
    assertEquals(3, minimizerValues.get(1).getOriginalIndex());

    assertEquals(1, minimizerValues.get(2).getValue().getHash());
    assertEquals(4, minimizerValues.get(2).getOriginalIndex());

    assertEquals(1, minimizerValues.get(3).getValue().getHash());
    assertEquals(5, minimizerValues.get(3).getOriginalIndex());
  }

  @Test
  public void minimizer_paperExample_shouldValidate() throws Exception {
    // given
    Minimizer minimizer = new Minimizer(5);
    List<Hash> hashes = Stream.of(77, 74, 17, 42, 97, 50, 17, 98, 6)
        .map(Hash::new).collect(Collectors.toList());

    // when
    List<MinimizerValue> minimizerValues = minimizer.minimize(hashes);

    // then
    // windows: [ <hash 17, index 2>, <hash 17, index 6>, <hash 6, index 8> ]
    assertEquals(3, minimizerValues.size());

    assertEquals(17, minimizerValues.get(0).getValue().getHash());
    assertEquals(2, minimizerValues.get(0).getOriginalIndex());

    assertEquals(17, minimizerValues.get(1).getValue().getHash());
    assertEquals(6, minimizerValues.get(1).getOriginalIndex());

    assertEquals(6, minimizerValues.get(2).getValue().getHash());
    assertEquals(8, minimizerValues.get(2).getOriginalIndex());
  }
}
