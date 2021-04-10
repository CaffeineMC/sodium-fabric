package me.jellysquid.mods.sodium.client.render.chunk.format;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2IntMaps;

import net.minecraft.block.BlockState;

public class MaterialIdHolder {
    private Map<BlockState, Integer> idMap;
    public short id;

    public MaterialIdHolder() {
        this.idMap = Object2IntMaps.emptyMap();
        this.id = -1;
    }

    public MaterialIdHolder(Map<BlockState, Integer> idMap) {
        this.idMap = idMap;
        this.id = -1;
    }

    public void set(BlockState state) {
        this.id = (short) this.idMap.getOrDefault(state, -1).intValue();
    }

    public void reset() {
        this.id = -1;
    }
}
