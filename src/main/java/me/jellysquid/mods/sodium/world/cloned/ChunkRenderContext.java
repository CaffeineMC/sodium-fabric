package me.jellysquid.mods.sodium.world.cloned;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkSectionPos;

public class ChunkRenderContext {
    private final ChunkSectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BlockBox volume;
    private final int chunkIndex;

    public ChunkRenderContext(ChunkSectionPos origin, ClonedChunkSection[] sections, BlockBox volume, int chunkIndex) {
        this.origin = origin;
        this.sections = sections;
        this.volume = volume;
        this.chunkIndex = chunkIndex;
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
        for (ClonedChunkSection section : sections) {
            if (section != null) {
                section.getBackingCache()
                        .release(section);
            }
        }
    }

    public int getRelativeChunkIndex() {
        return this.chunkIndex;
    }
}
