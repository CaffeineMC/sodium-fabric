package me.jellysquid.mods.sodium.client.render.backends.shader.lcb;

import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;

public class ShaderLCBRenderState implements ChunkRenderState {
    private final BufferSegment segment;

    public ShaderLCBRenderState(BufferSegment segment) {
        this.segment = segment;
    }

    @Override
    public void delete() {
        this.segment.delete();
    }

    public BufferSegment getSegment() {
        return this.segment;
    }
}
