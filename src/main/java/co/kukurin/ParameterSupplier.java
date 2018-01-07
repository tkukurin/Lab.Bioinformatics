package co.kukurin;

import java.util.function.Function;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.rabinfingerprint.polynomial.Polynomial;

@Builder
@Setter
@Getter
public class ParameterSupplier {

  private int sketchSize;
  private int windowSize;
  private int kmerSize;
  private double tau;
  private Polynomial fingerprintingPolynomial;
  private Function<String, byte[]> stringToByteArrayConverter;
}
