package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Super class for translucent data that contains an actual buffer.
 */
public abstract class PresentTranslucentData extends TranslucentData {
    private NativeBuffer buffer;
    private boolean reuseUploadedData;
    private int quadHash;
    private int length;

    PresentTranslucentData(ChunkSectionPos sectionPos, NativeBuffer buffer) {
        super(sectionPos);
        this.buffer = buffer;
        this.length = TranslucentData.indexBytesToQuadCount(buffer.getLength());
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

    void setQuadHash(int hash) {
        this.quadHash = hash;
    }

    int getQuadHash() {
        return this.quadHash;
    }

    int getLength() {
        return this.length;
    }

    public NativeBuffer getBuffer() {
        return this.buffer;
    }

    public boolean isReusingUploadedData() {
        return this.reuseUploadedData;
    }

    void setReuseUploadedData() {
        this.reuseUploadedData = true;
    }

    void unsetReuseUploadedData() {
        this.reuseUploadedData = false;
    }

    static NativeBuffer nativeBufferForQuads(TQuad[] quads) {
        return new NativeBuffer(TranslucentData.quadCountToIndexBytes(quads.length));
    }
}
