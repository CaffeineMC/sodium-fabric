package me.jellysquid.mods.sodium.client.render.chunk.compile;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadEncoder;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.model.quad.sink.ModelQuadSink;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.GlAllocationUtils;

import java.nio.ByteBuffer;

/**
 * An optimized resizeable buffer for writing rendered quad data that will be later used for chunk mesh rendering. Since
 * chunks only perform one kind of transformation (a translation), the expensive matrix operations can be eliminated.
 */
public class ChunkMeshBuilder implements ModelQuadSink {
    /**
     * The encoder used to serialize model quads into the specified vertex format for consumption by the graphics card.
     */
    private final ModelQuadEncoder encoder;

    /**
     * The collection of sprites used by the vertex data in this builder.
     */
    private ChunkRenderData.Builder renderData;

    /**
     * The size of each written quad in bytes. This is always 4 times the stride of the vertex format.
     */
    private final int stride;

    /**
     * The backing direct buffer in the current platform's native byte order which holds the buffered vertex data.
     */
    private ByteBuffer buffer;

    /**
     * The current pointer into the backing buffer which marks the head at which data will be written into next.
     */
    private int position;

    /**
     * The maximum capacity of the backing buffer in bytes before it needs to be resized.
     */
    private int capacity;

    /**
     * The translation to be applied to all quads written into this mesh builder.
     */
    private float x, y, z;

    /**
     * The scale to be applied to all offsets and quads written into this mesh builder.
     */
    private final float scale;

    private final BlockLayer layer;

    public ChunkMeshBuilder(GlVertexFormat<?> format, BlockLayer layer, int initialSize) {
        this.scale = 1.0f / 32.0f;
        this.stride = format.getStride() * 4;
        this.encoder = SodiumVertexFormats.getEncoder(format);

        this.buffer = GlAllocationUtils.allocateByteBuffer(initialSize);
        this.capacity = initialSize;
        this.layer = layer;
    }

    public void begin(ChunkRenderData.Builder renderData) {
        if (this.renderData != null) {
            throw new IllegalStateException("Not finished building!");
        }

        this.renderData = renderData;
    }

    public void setOffset(int x, int y, int z) {
        this.x = x * this.scale;
        this.y = y * this.scale;
        this.z = z * this.scale;
    }

    @Override
    public void write(ModelQuadViewMutable quad) {
        // Mark the write pointer we will be using
        int position = this.position;
        int len = this.stride;

        // Advance the write pointer by the number of bytes we're about to write
        this.position += len;

        // If the write pointer is now outside the capacity of the backing buffer, grow it to accomodate the incoming data
        if (this.position >= this.capacity) {
            this.grow(len);
        }

        // Translate the quad to its local position in the chunk
        for (int i = 0; i < 4; i++) {
            quad.setX(i, (quad.getX(i) * this.scale) + this.x);
            quad.setY(i, (quad.getY(i) * this.scale) + this.y);
            quad.setZ(i, (quad.getZ(i) * this.scale) + this.z);
        }

        // Write the quad to the backing buffer using the marked position from earlier
        this.encoder.write(quad, this.buffer, position, this.layer.isMipped());

        Sprite sprite = quad.getSprite();

        if (sprite != null) {
            this.renderData.addSprite(sprite);
        }
    }

    private void grow(int len) {
        // The new capacity will at least as large as the write it needs to service
        int cap = Math.max(this.capacity * 2, this.capacity + len);

        // Allocate a new buffer and copy the old buffer's contents into it
        ByteBuffer buffer = GlAllocationUtils.allocateByteBuffer(cap);
        buffer.put(this.buffer);
        buffer.position(0);

        // Update the buffer and capacity now
        this.buffer = buffer;
        this.capacity = cap;
    }

    /**
     * @return True if no vertex data exists in this buffer, otherwise false
     */
    public boolean isEmpty() {
        return this.position <= 0;
    }

    public void sortQuads(float x, float y, float z) {
        // TODO
    }

    /**
     * Ends the stream of written data and makes a copy of it to be passed around.
     */
    public void copyInto(ByteBuffer dst) {
        if (this.renderData != null) {
            throw new IllegalStateException("Not finished building");
        }

        // Mark the slice of memory that needs to be copied
        this.buffer.position(0);
        this.buffer.limit(this.position);

        // Allocate a new buffer which is just large enough to contain the slice of vertex data
        // The buffer is then flipped after the operation so the callee sees a range of bytes from (0,len] which can
        // then be immediately passed to native libraries or the graphics driver
        dst.put(this.buffer.slice());

        // Reset the position and limit set earlier of the backing scratch buffer
        this.buffer.clear();
        this.position = 0;
    }

    public int getSize() {
        return this.position;
    }

    public void finish() {
        this.renderData = null;
    }
}
