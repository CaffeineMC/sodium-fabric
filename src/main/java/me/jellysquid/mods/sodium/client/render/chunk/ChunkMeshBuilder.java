package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadConsumer;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadEncoder;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadViewMutable;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.GlAllocationUtils;

import java.nio.ByteBuffer;

public class ChunkMeshBuilder implements ModelQuadConsumer, VertexConsumer {
    private final GlVertexFormat<?> format;
    private final ModelQuadEncoder encoder;

    private final int stride;

    private ByteBuffer buffer;
    private int position;
    private int capacity;

    public ChunkMeshBuilder(GlVertexFormat<?> format, int initialSize) {
        this.format = format;
        this.stride = format.getStride();

        this.encoder = SodiumVertexFormats.getEncoder(format);

        this.buffer = GlAllocationUtils.allocateByteBuffer(initialSize);
        this.capacity = initialSize;
    }

    @Override
    public void write(ModelQuadViewMutable quad) {
        int position = this.position;
        int len = this.stride * 4;

        this.position += len;

        if (this.position >= this.capacity) {
            this.grow(len);
        }

        this.encoder.write(this.format, this.buffer, position, quad);
    }

    private void grow(int len) {
        int cap = Math.max(this.capacity * 2, this.capacity + len);

        ByteBuffer buffer = GlAllocationUtils.allocateByteBuffer(cap);
        buffer.put(this.buffer);
        buffer.position(0);

        this.buffer = buffer;
        this.capacity = cap;
    }

    public boolean isEmpty() {
        return this.position <= 0;
    }

    public void sortQuads(float x, float y, float z) {
        // TODO
    }

    public ByteBuffer end() {
        this.buffer.position(0);
        this.buffer.limit(this.position);

        ByteBuffer copy = GlAllocationUtils.allocateByteBuffer(this.buffer.limit());
        copy.put(this.buffer.slice());
        copy.flip();

        this.buffer.clear();
        this.position = 0;

        return copy;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VertexConsumer texture(float u, float v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VertexConsumer overlay(int u, int v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VertexConsumer light(int u, int v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VertexConsumer normal(float x, float y, float z) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void next() {
        throw new UnsupportedOperationException();
    }
}
