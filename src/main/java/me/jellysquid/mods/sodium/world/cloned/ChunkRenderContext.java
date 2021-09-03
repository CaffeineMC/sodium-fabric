package me.jellysquid.mods.sodium.world.cloned;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkSectionPos;

public class ChunkRenderContext {
    private final ChunkSectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BlockBox volume;
    private final short chunkIndex;

    public ChunkRenderContext(ChunkSectionPos origin, ClonedChunkSection[] sections, BlockBox volume, short chunkIndex) {
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

    public short getRelativeChunkIndex() {
        return this.chunkIndex;
    }
}
