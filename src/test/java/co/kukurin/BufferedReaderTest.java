package co.kukurin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import co.kukurin.FastaKmerBufferedReader.SequenceIterator;
import java.io.StringReader;
import org.junit.Test;

public class BufferedReaderTest {

  @Test
  public void testRead_withNewline_shouldIgnoreNewline() throws Exception {
    // given
    String dummySequence = ">header\n12345\n12345";
    int kmerSize = 5;
    FastaKmerBufferedReader reader = new FastaKmerBufferedReader(
        new StringReader(dummySequence), kmerSize);

    // when
    SequenceIterator sequenceIterator = reader.next();
    StringBuilder first = new StringBuilder();
    sequenceIterator.readNext().forEachRemaining(first::append);

    StringBuilder second = new StringBuilder();
    sequenceIterator.readNext().forEachRemaining(second::append);

    StringBuilder third = new StringBuilder();
    sequenceIterator.readNext().forEachRemaining(third::append);

    // then
    assertEquals("12345", first.toString());
    assertEquals("23451", second.toString());
    assertEquals("34512", third.toString());
  }

  @Test
  public void testRead_headerOnly() throws Exception {
    // given
    String dummySequence = ">header\n";
    int kmerSize = 5;
    FastaKmerBufferedReader reader = new FastaKmerBufferedReader(
        new StringReader(dummySequence), kmerSize);

    // when
    SequenceIterator sequenceIterator = reader.next();

    // then
    assertFalse(sequenceIterator.readNext().hasNext());
    assertFalse(reader.hasNext());
  }
}
