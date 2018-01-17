package co.kukurin.map;

import co.kukurin.hash.Minimizer.MinimizerValue;
import co.kukurin.hash.Hash;
import java.util.Collection;
import java.util.TreeMap;

/**
 * Class which is used to compute number of shared hashes between reference and query.
 */
class SketchMap {

  private static final int NOT_STORED = -1;

  static final class HashInfo {

    int queryPosition;
    int referencePosition;

    private HashInfo(int queryPosition, int referencePosition) {
      this.queryPosition = queryPosition;
      this.referencePosition = referencePosition;
    }

  }

  private final TreeMap<Hash, HashInfo> map = new TreeMap<>();

  SketchMap(Collection<MinimizerValue> queryHashes) {
    queryHashes.forEach(hash -> map.put(
        hash.getHash(),
        new HashInfo(hash.getOriginalIndex(), NOT_STORED)));
  }

  void putReference(MinimizerValue minimizerValue) {
    Hash referenceHash = minimizerValue.getHash();
    int referencePosition = minimizerValue.getOriginalIndex();

    HashInfo existing = map.get(referenceHash);
    if (existing == null) {
      map.put(referenceHash, new HashInfo(NOT_STORED, referencePosition));
    } else {
      existing.referencePosition = referencePosition;
    }
  }

  void removeReference(MinimizerValue minimizerValue) {
    Hash referenceHash = minimizerValue.getHash();
    int referencePosition = minimizerValue.getOriginalIndex();

    HashInfo existing = map.get(referenceHash);
    if (existing != null && existing.referencePosition == referencePosition) {
      if (existing.queryPosition == NOT_STORED) {
        map.remove(referenceHash);
      }

      existing.referencePosition = NOT_STORED;
    }
  }

  int getSharedMinimizers(int sketchSize) {
    return (int) map.values().stream().limit(sketchSize)
        .filter(info -> info.queryPosition != NOT_STORED && info.referencePosition != NOT_STORED)
        .count();
  }
}
