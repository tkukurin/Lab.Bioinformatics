package co.kukurin;

import static co.kukurin.Main.HASH_FUNCTION;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import java.nio.charset.Charset;
import org.junit.Assert;
import org.junit.Test;

public class HashingTest {

  private final HashFunction function = HASH_FUNCTION;

  @Test
  public void murmurHash_charArrayVsStream_shouldBeEqual() throws Exception {
    Hasher hasher1 = function.newHasher();
    Hasher hasher2 = function.newHasher();

    for (char b : "test string".toCharArray()) {
      hasher1.putChar(b);
    }

    long hash1 = hasher1.hash().asLong();

    "test string".chars().forEach(c -> hasher2.putChar((char) c));
    long hash2 = hasher2.hash().asLong();

    Assert.assertEquals(hash1, hash2);
  }

  @Test
  public void murmurHash_charArrayVsString_shouldNotBeEqual() throws Exception {
    Hasher hasher1 = function.newHasher();
    Hasher hasher2 = function.newHasher();

    for (char b : "test string".toCharArray()) {
      hasher1.putChar(b);
    }

    long hash1 = hasher1.hash().asLong();

    hasher2.putString("test string", Charset.defaultCharset());
    long hash2 = hasher2.hash().asLong();

    Assert.assertNotEquals(hash1, hash2);
  }
}
