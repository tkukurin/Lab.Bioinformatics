package co.kukurin.hash;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Hahcode abstraction.
 */
@EqualsAndHashCode
public class Hash implements Comparable<Hash> {

  @Getter
  private final long value;

  public Hash(long value) {
    this.value = value;
  }

  @Override
  public int compareTo(Hash o) {
    return Long.compare(value, o.getValue());
  }

}
