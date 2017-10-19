package co.kukurin;

import org.junit.Test;
import org.rabinfingerprint.polynomial.Polynomial;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HasherTest {

    @Test
    public void twoWindows_euqalContents_shouldHashEqual() throws Exception {
        // given
        String toHash = "AAAA";
        Hasher hasher = new Hasher(Polynomial.createIrreducible(52), 3);

        // when
        List<Hasher.Hash> result = hasher.hash(toHash);

        // then
        assertEquals(2, result.size());
        assertTrue(result.get(0).equals(result.get(1)));
    }

    @Test
    public void threeWindows_differentContents_shouldHashDifferent() throws Exception {
        // given
        String toHash = "AAABC";
        Hasher hasher = new Hasher(Polynomial.createIrreducible(5), 3);

        // when
        List<Hasher.Hash> result = hasher.hash(toHash);

        // then
        assertEquals(3, result.size());
        assertFalse(result.get(0).equals(result.get(1)));
    }

}
