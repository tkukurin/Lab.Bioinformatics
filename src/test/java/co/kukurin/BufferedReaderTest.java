package co.kukurin;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import org.junit.Test;

public class BufferedReaderTest {

  @Test
  public void testRead_withNewline_shouldIgnoreNewline() throws Exception {
    // given
    String dummySequence = "12345\n12345";
    int kmerSize = 5;
    FastaKmerBufferedReader reader = new FastaKmerBufferedReader(
        new StringReader(dummySequence), kmerSize);

    // when
    StringBuilder first = new StringBuilder();
    reader.readNext().forEachRemaining(first::append);

    StringBuilder second = new StringBuilder();
    reader.readNext().forEachRemaining(second::append);

    StringBuilder third = new StringBuilder();
    reader.readNext().forEachRemaining(third::append);

    // then
    assertEquals("12345", first.toString());
    assertEquals("23451", second.toString());
    assertEquals("34512", third.toString());
  }
}
