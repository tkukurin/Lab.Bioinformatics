package co.kukurin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import co.kukurin.fasta.FastaKmerBufferedReader;
import co.kukurin.fasta.FastaKmerBufferedReader.KmerSequenceGenerator;
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
    KmerSequenceGenerator kmerSequenceGenerator = reader.next().get();
    StringBuilder first = new StringBuilder();
    kmerSequenceGenerator.readNext().forEachRemaining(first::append);

    StringBuilder second = new StringBuilder();
    kmerSequenceGenerator.readNext().forEachRemaining(second::append);

    StringBuilder third = new StringBuilder();
    kmerSequenceGenerator.readNext().forEachRemaining(third::append);

    // then
    assertEquals("12345", first.toString());
    assertEquals("23451", second.toString());
    assertEquals("34512", third.toString());
  }

  @Test
  public void testReadLength_withNewline_shouldIgnoreNewline() throws Exception {
    // given
    String dummySequence = ">header\n12345\n6789\n>";
    int kmerSize = 5;
    FastaKmerBufferedReader reader = new FastaKmerBufferedReader(
        new StringReader(dummySequence), kmerSize);

    // when
    KmerSequenceGenerator kmerSequenceGenerator = reader.next().get();
    while (kmerSequenceGenerator.readNext().hasNext()) {

    }

    // then
    assertEquals(9, kmerSequenceGenerator.totalReadBytes());
  }

  @Test
  public void testRead_headerOnly() throws Exception {
    // given
    String dummySequence = ">header\n";
    int kmerSize = 5;
    FastaKmerBufferedReader reader = new FastaKmerBufferedReader(
        new StringReader(dummySequence), kmerSize);

    // when
    KmerSequenceGenerator kmerSequenceGenerator = reader.next().get();

    // then
    assertFalse(kmerSequenceGenerator.readNext().hasNext());
  }

}
