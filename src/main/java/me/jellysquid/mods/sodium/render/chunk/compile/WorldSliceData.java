package me.jellysquid.mods.sodium.render.chunk.compile;

import me.jellysquid.mods.sodium.world.cloned.ClonedChunkSection;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkSectionPos;

public class WorldSliceData {
    private final ChunkSectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BlockBox volume;

    public WorldSliceData(ChunkSectionPos origin, ClonedChunkSection[] sections, BlockBox volume) {
        this.origin = origin;
        this.sections = sections;
        this.volume = volume;
    }

    public ClonedChunkSection[] getSections() {
        return this.sections;
    }

    public ChunkSectionPos getOrigin() {
        return this.origin;
    }

    public BlockBox getVolume() {
        return this.volume;
    }

    public void releaseResources() {
        for (ClonedChunkSection section : this.sections) {
            if (section != null) {
                section.getBackingCache()
                        .release(section);
            }
        }
    }
}
