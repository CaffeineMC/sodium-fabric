package net.caffeinemc.sodium.world.slice.cloned.palette;

public class ClonedPalleteArray<K> implements ClonedPalette<K> {
    private final K[] array;

    public ClonedPalleteArray(K[] array) {
        this.array = array;
    }

    @Override
    public K get(int id) {
        return this.array[id];
    }
}
