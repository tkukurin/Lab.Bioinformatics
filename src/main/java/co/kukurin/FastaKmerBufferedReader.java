package co.kukurin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

public class FastaKmerBufferedReader {

  private final BufferedReader bufferedReader;
  private final int kmerSize;

  private char[] values;
  private int valuesIter = -1;

  private String header;

  public FastaKmerBufferedReader(Reader reader, int kmerSize) {
    this.bufferedReader = new BufferedReader(reader);
    this.kmerSize = kmerSize;
  }

  public Iterator<Character> nextKmer() throws IOException {
    int readValue = nextNonWhitespace();

    if (readValue == -1) {
      return emptyIterator();
    }

    if (readValue == '>') {
      header = bufferedReader.readLine().trim();
      return nextKmer();
    }

    if (values == null) {
      values = new char[kmerSize];
      values[0] = (char) readValue;
      int size = bufferedReader.read(values, 1, kmerSize - 1);

      if (size < kmerSize - 1) {
        throw new RuntimeException("Read not large enough");
      }

      return charBufIterator(0);
    }

    valuesIter = (valuesIter + 1) % kmerSize;
    values[valuesIter] = (char) readValue;

    return charBufIterator((valuesIter + 1) % kmerSize);
  }

  private int nextNonWhitespace() throws IOException {
    int readValue = bufferedReader.read();

    while (Character.isWhitespace(readValue)) {
      readValue = bufferedReader.read();
    }

    return readValue;
  }

  private Iterator<Character> charBufIterator(int start) {
    return new Iterator<Character>() {
      int nRead = 0;
      int i = (start + kmerSize - 1) % kmerSize;

      @Override
      public boolean hasNext() {
        return nRead < kmerSize;
      }

      @Override
      public Character next() {
        nRead++;
        i = (i + 1) % values.length;
        return values[i];
      }
    };
  }

  private Iterator<Character> emptyIterator() {
    return new Iterator<Character>() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public Character next() {
        return null;
      }
    };
  }

}
