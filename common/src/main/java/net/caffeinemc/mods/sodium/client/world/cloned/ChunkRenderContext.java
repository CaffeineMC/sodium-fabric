package net.caffeinemc.mods.sodium.client.world.cloned;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class ChunkRenderContext {
    private final SectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BoundingBox volume;
    private final Object modelData;

    public ChunkRenderContext(SectionPos origin, ClonedChunkSection[] sections, BoundingBox volume, Object modelData) {
        this.origin = origin;
        this.sections = sections;
        this.volume = volume;
        this.modelData = modelData;
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

    public Object getModelData() {
        return modelData;
    }
}
