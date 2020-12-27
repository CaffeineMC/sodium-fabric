package me.jellysquid.mods.sodium.mixin.core.pipeline;

import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.model.vertex.VertexSinkFactory;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.util.UnsafeUtil;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.GlAllocationUtils;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder implements VertexBufferView, VertexDrain {
    @Shadow
    private int elementOffset;

    @Shadow
    private ByteBuffer buffer;

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    private static int roundBufferSize(int amount) {
        throw new UnsupportedOperationException();
    }

    @Shadow
    private VertexFormat format;

    @Shadow
    private int vertexCount;

    @Override
    public boolean ensureBufferCapacity(int size) {
        if (this.elementOffset + size <= this.buffer.capacity()) {
            return false;
        }

        int newSize = this.buffer.capacity() + roundBufferSize(size);

        LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", this.buffer.capacity(), newSize);

        this.buffer.position(0);

        ByteBuffer byteBuffer = GlAllocationUtils.allocateByteBuffer(newSize);
        byteBuffer.put(this.buffer);
        byteBuffer.rewind();

        this.buffer = byteBuffer;

        return true;
    }

    @Override
    public ByteBuffer getDirectBuffer() {
        return this.buffer;
    }

    @Override
    public int getElementOffset() {
        return this.elementOffset;
    }

    @Override
    public VertexFormat getVertexFormat() {
        return this.format;
    }

    @Override
    public void flush(int vertices, VertexFormat format) {
        if (this.format != format) {
            throw new IllegalStateException("Mis-matched vertex format (expected: [" + format + "], currently using: [" + this.format + "])");
        }

        this.vertexCount += vertices;
        this.elementOffset += vertices * format.getVertexSize();
    }

    @Override
    public <T extends VertexSink> T createSink(VertexSinkFactory<T> factory) {
        return factory.createBufferWriter(this, UnsafeUtil.isAvailable());
    }
}
