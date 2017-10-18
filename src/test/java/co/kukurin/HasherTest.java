package co.kukurin;

import co.kukurin.model.Hash;
import org.junit.Test;

import java.util.List;

public class HasherTest {

    @Test
    public void shouldHash() throws Exception {
        // given
        String toHash = "AAAA";
        Hasher hasher = new Hasher(3);

        // when
        List<Hash> result = hasher.hash(toHash);

        // then
        System.out.println(result);
    }
}
