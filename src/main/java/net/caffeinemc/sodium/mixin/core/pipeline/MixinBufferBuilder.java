package net.caffeinemc.sodium.mixin.core.pipeline;

import net.caffeinemc.gfx.api.buffer.BufferVertexFormat;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.render.vertex.VertexDrain;
import net.caffeinemc.sodium.render.vertex.VertexSink;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferView;
import net.caffeinemc.sodium.render.vertex.type.BlittableVertexType;
import net.caffeinemc.sodium.render.vertex.type.VertexType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.GlAllocationUtils;
import org.slf4j.Logger;
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
    public boolean ensureBufferCapacity(int bytes) {
        // Ensure that there is always space for 1 more vertex; see BufferBuilder.next()
        bytes += this.format.getVertexSize();

        if (this.elementOffset + bytes <= this.buffer.capacity()) {
            return false;
        }

        int newSize = this.buffer.capacity() + roundBufferSize(bytes);

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
    public int getWriterPosition() {
        return this.elementOffset;
    }

    @Override
    public BufferVertexFormat getVertexFormat() {
        return (BufferVertexFormat) this.format;
    }

    @Override
    public void flush(int vertexCount, BufferVertexFormat format) {
        if (this.getVertexFormat() != format) {
            throw new IllegalStateException("Mis-matched vertex format (expected: [" + format + "], currently using: [" + this.format + "])");
        }

        this.vertexCount += vertexCount;
        this.elementOffset += vertexCount * format.stride();
    }

    @Override
    public <T extends VertexSink> T createSink(VertexType<T> factory) {
        BlittableVertexType<T> blittable = factory.asBlittable();

        if (blittable != null && blittable.getBufferVertexFormat() == this.getVertexFormat())  {
            return blittable.createBufferWriter(this, SodiumClientMod.isDirectMemoryAccessEnabled());
        }

        return factory.createFallbackWriter((VertexConsumer) this);
    }
}
