package me.jellysquid.mods.sodium.client.world.cloned.palette;

import org.apache.commons.lang3.Validate;

public class ClonedPalleteArray<K> implements ClonedPalette<K> {
    private final K[] array;

    public ClonedPalleteArray(K[] array) {
        this.array = array;
    }

    @Override
    public K get(int id) {
        // TODO: Remove this check?
        return Validate.notNull(this.array[id]);
    }
}
