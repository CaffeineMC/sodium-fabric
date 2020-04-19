package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadConsumer;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadEncoder;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadViewMutable;
import net.minecraft.client.util.GlAllocationUtils;

import java.nio.ByteBuffer;

public class ChunkMeshBuilder implements ModelQuadConsumer {
    private final GlVertexFormat<?> format;
    private final ModelQuadEncoder encoder;

    private final int stride;

    private ByteBuffer buffer;
    private int position;
    private int capacity;

    private float x, y, z;

    public ChunkMeshBuilder(GlVertexFormat<?> format, int initialSize) {
        this.format = format;
        this.stride = format.getStride();

        this.encoder = SodiumVertexFormats.getEncoder(format);

        this.buffer = GlAllocationUtils.allocateByteBuffer(initialSize);
        this.capacity = initialSize;
    }

    public void setOffset(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void write(ModelQuadViewMutable quad) {
        int position = this.position;
        int len = this.stride * 4;

        this.position += len;

        if (this.position >= this.capacity) {
            this.grow(len);
        }

        this.encoder.write(this.format, this.buffer, position, quad, this.x, this.y, this.z);
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
}
