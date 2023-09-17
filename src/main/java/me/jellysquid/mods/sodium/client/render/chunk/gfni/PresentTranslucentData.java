package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

public abstract class PresentTranslucentData extends TranslucentData {
    public NativeBuffer buffer;

    public PresentTranslucentData(ChunkSectionPos sectionPos, NativeBuffer buffer) {
        super(sectionPos);
        this.buffer = buffer;
    }

    @Override
    public void delete() {
        super.delete();
        if (this.buffer != null) {
            this.buffer.free();
            this.buffer = null;
        }
    }
}
