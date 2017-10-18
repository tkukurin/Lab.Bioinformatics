package co.kukurin.model;

import lombok.Value;

@Value
public class Hash implements Comparable<Hash> {
    private long hash;

    @Override
    public int compareTo(Hash o) {
        return Long.compare(hash, o.getHash());
    }
}
