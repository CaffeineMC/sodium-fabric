package me.jellysquid.mods.sodium.client.world.cloned;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class ChunkRenderContext {
    private final SectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BoundingBox volume;

    public ChunkRenderContext(SectionPos origin, ClonedChunkSection[] sections, BoundingBox volume) {
        this.origin = origin;
        this.sections = sections;
        this.volume = volume;
    }

    public ClonedChunkSection[] getSections() {
        return this.sections;
    }

    public SectionPos getOrigin() {
        return this.origin;
    }

    public BoundingBox getVolume() {
        return this.volume;
    }
}
