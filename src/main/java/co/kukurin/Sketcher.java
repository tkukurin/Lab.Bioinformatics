package co.kukurin;

import co.kukurin.Minimizer.MinimizerValue;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Sketcher {

  private final int sketchSize;

  public List<MinimizerValue> sketch(List<MinimizerValue> minimizerValues) {
    return minimizerValues.stream().distinct().limit(sketchSize).collect(Collectors.toList());
  }
}
