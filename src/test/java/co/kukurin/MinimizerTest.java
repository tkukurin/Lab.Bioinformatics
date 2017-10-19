package co.kukurin;

import co.kukurin.Minimizer.MinimizerValue;
import co.kukurin.model.Hash;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class MinimizerTest {

    @Test
    public void uniqueValues_returnsSmallestInWindow() throws Exception {
        // given
        Minimizer minimizer = new Minimizer(2);
        List<Hash> hashes = Stream.of(1L, 2L, 1L, 5L)
                .map(Hash::new)
                .collect(Collectors.toList());

        // when
        List<MinimizerValue> values = minimizer.minimize(hashes);

        // then
        assertEquals(3, values.size());
        assertEquals(0, values.get(0).getIndex());
        assertEquals(2, values.get(1).getIndex());
        assertEquals(2, values.get(2).getIndex());
    }

    @Test
    public void multipleSmallest_returnsRightmostIndex() throws Exception {
        // given
        Minimizer minimizer = new Minimizer(2);
        List<Hash> hashes = Stream.of(1L, 1L, 1L)
                .map(Hash::new)
                .collect(Collectors.toList());

        // when
        List<MinimizerValue> values = minimizer.minimize(hashes);

        // then
        assertEquals(2, values.size());
        assertEquals(1, values.get(0).getIndex());
        assertEquals(2, values.get(1).getIndex());
    }

}
