package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * TODO: take care of deleting the native buffers when the translucent data
 * stored here is replaced.
 */
public abstract class PresentTranslucentData extends TranslucentData {
    public final NativeBuffer buffer;

    public PresentTranslucentData(ChunkSectionPos sectionPos, NativeBuffer buffer) {
        super(sectionPos);
        this.buffer = buffer;
    }
}
