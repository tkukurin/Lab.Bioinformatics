package co.kukurin;

import co.kukurin.Minimizer.MinimizerValue;
import co.kukurin.ReadHasher.Hash;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

public class SketchMap {

  public static final int NOT_STORED = -1;

  static final class HashInfo {

    int queryPosition;
    int referencePosition;

    public HashInfo(int queryPosition, int referencePosition) {
      this.queryPosition = queryPosition;
      this.referencePosition = referencePosition;
    }

  }
//  private final Set<Hash> queryHashes;
//  private final List<MinimizerValue> referenceMinimizers = new LinkedList<>();

  private final TreeMap<Hash, HashInfo> map = new TreeMap<>();

  public SketchMap(Collection<MinimizerValue> queryHashes) {
    queryHashes.forEach(hash -> map.put(
        hash.getValue(),
        new HashInfo(hash.getOriginalIndex(), NOT_STORED)));
  }

  public void putReference(MinimizerValue minimizerValue) {
    Hash referenceHash = minimizerValue.getValue();
    int referencePosition = minimizerValue.getOriginalIndex();

    HashInfo existing = map.get(referenceHash);
    if (existing == null) {
      map.put(referenceHash, new HashInfo(NOT_STORED, referencePosition));
    } else {
      if (existing.referencePosition == NOT_STORED) {
//        sketchesFromUnion++;
      }

      existing.referencePosition = referencePosition;
    }
  }

  public void removeReference(MinimizerValue minimizerValue) {
    Hash referenceHash = minimizerValue.getValue();
    int referencePosition = minimizerValue.getOriginalIndex();

    HashInfo existing = map.get(referenceHash);
    if (existing == null) {
      // TODO ?
    } else if (existing.referencePosition == referencePosition) {
      if (existing.queryPosition == NOT_STORED) {
        map.remove(referenceHash);
      }

      existing.referencePosition = NOT_STORED;
    }
  }

  public int getSharedMinimizers(int sketchSize) {
    return (int) map.values().stream().limit(sketchSize)
        .filter(info -> info.queryPosition != NOT_STORED && info.referencePosition != NOT_STORED)
        .count();
  }
}
