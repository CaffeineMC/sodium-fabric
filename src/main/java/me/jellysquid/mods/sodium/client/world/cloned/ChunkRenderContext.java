package me.jellysquid.mods.sodium.client.world.cloned;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;

public class ChunkRenderContext {
    private final ChunkSectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BlockBox volume;

    public ChunkRenderContext(ChunkSectionPos origin, ClonedChunkSection[] sections, BlockBox volume) {
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
        for (ClonedChunkSection section : sections) {
            if (section != null) {
                section.getBackingCache()
                        .release(section);
            }
        }
    }
}
