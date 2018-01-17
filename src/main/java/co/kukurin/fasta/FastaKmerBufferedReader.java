package co.kukurin.fasta;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Optional;
import lombok.Getter;

/**
 * Buffered reader for efficient reading of k-mers from a file.
 *
 * <p>Reader returns {@link KmerSequenceGenerator} instances on each new invocation of the {@link
 * #next()} method.
 */
public class FastaKmerBufferedReader implements AutoCloseable {

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

  /**
   * Generator of k-mer sequences from {@link FastaKmerBufferedReader}'s internal {@link
   * BufferedReader}. E.g. for a sequence read "AAT" and k-mer size of 2, this generator will return
   * iterators over the following characters:
   *
   * <ol> <li>(A, A)</li> <li>(A, T)</li> </ol>
   */
  public class KmerSequenceGenerator {

    @Getter
    private String header;
    private char[] values;
    private int valuesIter;
    private int totalReadBytes;

    private KmerSequenceGenerator(String header) {
      this.header = header;
      this.totalReadBytes = 0;
      this.valuesIter = -1;
    }

    /**
     * @return next k-mer from read sequence. If there are no more k-mers to be read, will return
     * iterator that always returns false to hasNext calls.
     */
    public Iterator<Character> readNext() throws IOException {
      int readValue = nextNonWhitespace();

      if (readValue == '>') {
        bufferedReader.reset();
        return EMPTY_ITERATOR;
      }

      if (readValue == -1) {
        return EMPTY_ITERATOR;
      }

      if (values == null) {
        values = new char[kmerSize];
        values[0] = (char) readValue;

        int size = bufferedReader.read(values, 1, kmerSize - 1);
        if (size < kmerSize - 1) {
          throw new IOException("Read not large enough (k=" + kmerSize + ")");
        }

        totalReadBytes = kmerSize;
        return charBufIterator(0);
      }

      valuesIter = (valuesIter + 1) % kmerSize;
      values[valuesIter] = (char) readValue;
      totalReadBytes++;
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

    /**
     * @return total read bytes from sequence so far.
     */
    public int totalReadBytes() {
      return totalReadBytes;
    }
  }

  private final BufferedReader bufferedReader;
  private final int kmerSize;

  public FastaKmerBufferedReader(Reader reader, int kmerSize) throws FileNotFoundException {
    this.bufferedReader = new BufferedReader(reader);
    this.kmerSize = kmerSize;
  }

  /**
   * @return next {@link KmerSequenceGenerator} instance. Will read a header line and return a
   * {@link KmerSequenceGenerator} which reads k-mers in a streaming fashion from the source.
   *
   * <p>The returned {@link KmerSequenceGenerator} instance must be exhausted before calling {@link
   * #next()} a second time, otherwise behavior is undefined.
   */
  public Optional<KmerSequenceGenerator> next() {
    try {
      int readValue = nextNonWhitespace();

      if (readValue == -1) {
        return Optional.empty();
      }

      if (readValue == '>') {
        return Optional.of(new KmerSequenceGenerator(bufferedReader.readLine().trim()));
      }

      throw new IOException("Bad file format");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() throws Exception {
    this.bufferedReader.close();
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
