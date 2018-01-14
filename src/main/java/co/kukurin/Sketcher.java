package co.kukurin;

import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Sketcher {

  private final int sketchSize;

  public <T> List<T> sketch(List<T> minimizerValues) {
    return minimizerValues.stream().distinct().limit(sketchSize).collect(Collectors.toList());
  }
}
