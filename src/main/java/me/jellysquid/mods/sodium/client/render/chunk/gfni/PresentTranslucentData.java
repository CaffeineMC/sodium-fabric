package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

public abstract class PresentTranslucentData extends TranslucentData {
    public NativeBuffer buffer;

    PresentTranslucentData(ChunkSectionPos sectionPos, NativeBuffer buffer) {
        super(sectionPos);
        this.buffer = buffer;
    }

    public abstract VertexRange[] getVertexRanges();

    @Override
    public void delete() {
        super.delete();
        if (this.buffer != null) {
            this.buffer.free();
            this.buffer = null;
        }
    }

    static NativeBuffer nativeBufferForQuads(TQuad[] quads) {
        return new NativeBuffer(TranslucentData.quadCountToIndexBytes(quads.length));
    }
}
