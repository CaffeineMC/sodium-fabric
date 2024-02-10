package net.caffeinemc.mods.sodium.client.world.cloned;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The render context for a chunk rebuild task, which provides an immutable, thread-safe copy of all relevant data used
 * in chunk meshing.
 */
public class ChunkRenderContext {
    private final SectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BoundingBox box;

    public ChunkRenderContext(SectionPos origin, ClonedChunkSection[] sections, BoundingBox box) {
        this.origin = origin;
        this.sections = sections;
        this.box = box;
    }

    public ClonedChunkSection[] getSections() {
        return this.sections;
    }

    public SectionPos getOrigin() {
        return this.origin;
    }

    public BoundingBox getBox() {
        return this.box;
    }
}
