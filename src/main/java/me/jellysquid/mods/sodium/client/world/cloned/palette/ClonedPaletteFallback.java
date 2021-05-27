package me.jellysquid.mods.sodium.client.world.cloned.palette;

import net.minecraft.util.collection.IdList;

public class ClonedPaletteFallback<K> implements ClonedPalette<K> {
    private final IdList<K> idList;

    public ClonedPaletteFallback(IdList<K> idList) {
        this.idList = idList;
    }

    @Override
    public K get(int id) {
        return this.idList.get(id);
    }
}
