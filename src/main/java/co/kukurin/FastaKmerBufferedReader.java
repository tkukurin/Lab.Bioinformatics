package co.kukurin;

import co.kukurin.FastaKmerBufferedReader.SequenceIterator;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import lombok.Getter;

/**
 * Buffered reader for efficient reading of k-mers from a file.
 */
public class FastaKmerBufferedReader implements Iterator<SequenceIterator> {

  private static final Iterator<Character> EMPTY_ITERATOR = new Iterator<Character>() {
    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public Character next() {
      return null;
    }
  };

  class SequenceIterator {
    @Getter
    private String header;

    private char[] values;
    private int valuesIter = -1;

    private SequenceIterator(String header) {
      this.header = header;
    }

    Iterator<Character> readNext() throws IOException {
      int readValue = nextNonWhitespace();

      if (readValue == '>') {
        bufferedReader.reset();
        return EMPTY_ITERATOR;
      }

      if (readValue == -1) {
        eof = true;
        return EMPTY_ITERATOR;
      }

      if (values == null) {
        values = new char[kmerSize];
        values[0] = (char) readValue;
        int size = bufferedReader.read(values, 1, kmerSize - 1);

        if (size < kmerSize - 1) {
          throw new IOException("Read not large enough (k=" + kmerSize + ")");
        }

        return charBufIterator(0);
      }

      valuesIter = (valuesIter + 1) % kmerSize;
      values[valuesIter] = (char) readValue;

      return charBufIterator((valuesIter + 1) % kmerSize);
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
  }

  private final BufferedReader bufferedReader;
  private final int kmerSize;
  private boolean eof;

  public FastaKmerBufferedReader(Reader reader, int kmerSize) throws FileNotFoundException {
    this.bufferedReader = new BufferedReader(reader);
    this.kmerSize = kmerSize;
    this.eof = false;
  }

  @Override
  public boolean hasNext() {
    return !eof;
  }

  /**
   * @return next k-mer obtained from file, e.g. contents of "abc" with k-mer = 2 will return (a,
   * b), (b, c). If there are no more items to be read, will return an iterator whose
   * Iterator#hasNext value immediately returns false.
   */
  @Override
  public SequenceIterator next() {
    try {
      int readValue = nextNonWhitespace();

      if (readValue == -1) {
        throw new IOException("Reached end of file");
      }

      if (readValue == '>') {
        return new SequenceIterator(bufferedReader.readLine().trim());
      }

      throw new IOException("Bad file format");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private int nextNonWhitespace() throws IOException {
    int readValue = bufferedReader.read();

    while (Character.isWhitespace(readValue)) {
      bufferedReader.mark(2);
      readValue = bufferedReader.read();
    }

    return readValue;
  }


}
