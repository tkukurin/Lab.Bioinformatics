package co.kukurin.test;

import co.kukurin.FastaBufferedReader;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.util.Iterator;

public class BRTest {

  public static void main(String[] args) throws IOException {
    FastaBufferedReader r = new FastaBufferedReader("genomes/clostridium/query-1.fq.fa", 16);
    HashFunction hashFunction = Hashing.murmur3_32();

    int i = 0;
    for (Iterator<Character> iterable = r.readNext(); iterable.hasNext(); ) {
      Hasher hasher = hashFunction.newHasher();

      r.readNext().forEachRemaining(c -> {
        System.out.print(c);
        hasher.putChar(c);
      });

      System.out.println();
      System.out.println(hasher.hash().toString());
      System.out.println();

      if (i++ > 5) {
        break;
      }
    }
  }

}
