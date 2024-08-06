package net.caffeinemc.mods.sodium.client.world.cloned;

import net.caffeinemc.mods.sodium.client.services.SodiumModelDataContainer;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

public class ChunkRenderContext {
    private final SectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BoundingBox volume;
    private final List<?> renderers;

    public ChunkRenderContext(SectionPos origin, ClonedChunkSection[] sections, BoundingBox volume, List<?> renderers) {
        this.origin = origin;
        this.sections = sections;
        this.volume = volume;
        this.renderers = renderers;
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

    public List<?> getRenderers() {
        return renderers;
    }
}
