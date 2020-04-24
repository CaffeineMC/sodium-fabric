package me.jellysquid.mods.sodium.client.render.backends.shader.lcb;

import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.memory.BufferBlock;
import me.jellysquid.mods.sodium.client.gl.util.MultiDrawBatch;
import net.minecraft.util.math.ChunkSectionPos;

public class ChunkRegion {
    private final ChunkSectionPos origin;
    private final MultiDrawBatch batch;
    private final BufferBlock buffer;

    public ChunkRegion(ChunkSectionPos origin, int size) {
        this.origin = origin;
        this.buffer = new BufferBlock();
        this.batch = new MultiDrawBatch(size);
    }

    public ChunkSectionPos getOrigin() {
        return this.origin;
    }

    public BufferBlock getBuffer() {
        return this.buffer;
    }

    public boolean isEmpty() {
        return this.buffer.isEmpty();
    }

    public void delete() {
        this.buffer.delete();
    }

    public void addToBatch(ShaderLCBRenderState state) {
        this.batch.add(state.getStart(), state.getLength());
    }

    public GlVertexArray drawBatch(GlVertexAttributeBinding[] attributes) {
        GlVertexArray array = this.buffer.bind(attributes);
        this.batch.draw();

        return array;
    }

    public boolean isBatchEmpty() {
        return this.batch.isEmpty();
    }
}
