package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializer;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializerCache;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.GlAllocationUtils;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class MixinBufferBuilder implements VertexBufferWriter {
    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private int vertexCount;

    @Shadow
    private int elementOffset;

    @Shadow
    private native static int roundBufferSize(int amount);

    @Shadow
    @Final
    private static Logger LOGGER;

    private VertexFormatDescription formatDesc;

    private VertexFormatDescription lastSerializerFormat;
    private VertexSerializer lastSerializer;

    @Inject(method = "setFormat", at = @At("RETURN"))
    private void onFormatChanged(VertexFormat format, CallbackInfo ci) {
        this.formatDesc = VertexFormatRegistry.get(format);

        this.lastSerializerFormat = null;
        this.lastSerializer = null;
    }

    @Override
    public void push(long src, int count, VertexFormatDescription format) {
        var length = count * this.formatDesc.stride;

        this.ensureBufferCapacity(length);

        var dst = MemoryUtil.memAddress(this.buffer, this.elementOffset);

        if (this.formatDesc == format) {
            this.pushFast(src, dst, format, count);
        } else {
            this.pushSlow(src, dst, format, count);
        }

        this.vertexCount += count;
        this.elementOffset += length;
    }

    private void pushFast(long src, long dst, VertexFormatDescription format, int count) {
        MemoryUtil.memCopy(src, dst, count * format.stride);
    }

    private void pushSlow(long src, long dst, VertexFormatDescription format, int count) {
        if (this.lastSerializerFormat != format) {
            this.lastSerializerFormat = format;
            this.lastSerializer = VertexSerializerCache.get(format, this.formatDesc);
        }

        this.lastSerializer.serialize(src, dst, count);
    }

    private void ensureBufferCapacity(int bytes) {
        // Ensure that there is always space for 1 more vertex; see BufferBuilder.next()
        bytes += this.formatDesc.stride;

        if (this.elementOffset + bytes <= this.buffer.capacity()) {
            return;
        }

        int newSize = this.buffer.capacity() + roundBufferSize(bytes);

        LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", this.buffer.capacity(), newSize);

        ByteBuffer byteBuffer = GlAllocationUtils.resizeByteBuffer(this.buffer, newSize);
        byteBuffer.rewind();

        this.buffer = byteBuffer;
    }
}
