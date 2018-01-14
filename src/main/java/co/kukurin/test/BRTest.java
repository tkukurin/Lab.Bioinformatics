package co.kukurin.test;

import co.kukurin.FastaKmerBufferedReader;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

public class BRTest {

  public static void main(String[] args) throws IOException {
    FastaKmerBufferedReader r = new FastaKmerBufferedReader(new FileReader("genomes/clostridium/query-1.fq.fa"), 16);
    HashFunction hashFunction = Hashing.murmur3_32();

    int i = 0;
    for (Iterator<Character> iterable = r.nextKmer(); iterable.hasNext(); ) {
      Hasher hasher = hashFunction.newHasher();

      r.nextKmer().forEachRemaining(c -> {
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
