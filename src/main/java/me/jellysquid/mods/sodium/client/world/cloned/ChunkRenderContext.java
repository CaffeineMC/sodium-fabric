package me.jellysquid.mods.sodium.client.world.cloned;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.Nullable;

public class ChunkRenderContext {
    private final ChunkSectionPos origin;
    private final @Nullable ClonedChunkSection[] sections;
    private final BlockBox volume;

    public ChunkRenderContext(ChunkSectionPos origin, @Nullable ClonedChunkSection[] sections, BlockBox volume) {
        this.origin = origin;
        this.sections = sections;
        this.volume = volume;
    }

    public @Nullable ClonedChunkSection[] getSections() {
        return this.sections;
    }

    public ChunkSectionPos getOrigin() {
        return this.origin;
    }

    public BlockBox getVolume() {
        return this.volume;
    }

    public void releaseResources() {
        for (var section : this.sections) {
            if (section != null) {
                section.releaseReference();
            }
        }
    }
}
