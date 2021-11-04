package me.jellysquid.mods.sodium.client.gl.arena;

import me.jellysquid.mods.sodium.client.util.NativeBuffer;

public class PendingUpload {
    private final NativeBuffer data;
    private GlBufferSegment result;

    public PendingUpload(NativeBuffer data) {
        this.data = data;
    }

    public NativeBuffer getDataBuffer() {
        return this.data;
    }

    protected void setResult(GlBufferSegment result) {
        if (this.result != null) {
            throw new IllegalStateException("Result already provided");
        }

        this.result = result;
    }

    public GlBufferSegment getResult() {
        if (this.result == null) {
            throw new IllegalStateException("Result not computed");
        }

        return this.result;
    }

    public int getLength() {
        return this.data.getLength();
    }
}
