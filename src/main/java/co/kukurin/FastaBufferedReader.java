package co.kukurin;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

public class FastaBufferedReader {

  private final BufferedReader bufferedReader;
  private final int kmerSize;

  private char[] values;
  private int valuesIter = -1;

  public FastaBufferedReader(String filename, int kmerSize) throws FileNotFoundException {
    this.bufferedReader = new BufferedReader(new FileReader(filename));
    this.kmerSize = kmerSize;
  }

  public Iterator<Character> readNext() throws IOException {
    int readValue = bufferedReader.read();

    if (readValue == -1) {
      return emptyIterator();
    }

    if (readValue == '>') {
      bufferedReader.readLine();
      return readNext();
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
    return charBufIterator(valuesIter);
  }

  private Iterator<Character> charBufIterator(int start) {
    return new Iterator<Character>() {
      int nRead = 0;
      int i = start - 1;

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
