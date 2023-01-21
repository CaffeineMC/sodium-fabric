package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializerCache;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.GlAllocationUtils;
import org.lwjgl.system.MemoryStack;
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

    private VertexFormatDescription dstFormat;
    private int dstStride;

    @Inject(method = "setFormat", at = @At("RETURN"))
    private void onFormatChanged(VertexFormat format, CallbackInfo ci) {
        this.dstFormat = VertexFormatRegistry.get(format);
        this.dstStride = format.getVertexSizeByte();
    }

    @Override
    public long buffer(MemoryStack stack, int count, int stride, VertexFormatDescription format) {
        var length = count * stride;

        if (this.isMatchingLayout(format, stride)) {
            // We need to make sure there is enough space in the destination buffer now,
            // since the caller is going to write directly into us
            this.ensureBufferCapacity(length);

            return MemoryUtil.memAddress(this.buffer, this.elementOffset);
        }

        return stack.nmalloc(length);
    }

    @Override
    public void push(long src, int count, int stride, VertexFormatDescription format) {
        var len = count * this.dstStride;
        var dst = MemoryUtil.memAddress(this.buffer, this.elementOffset);

        // If the source buffer is the same as the destination buffer, no copy is necessary
        if (src != dst) {
            // Make sure there's enough space in the destination buffer for the copy
            this.ensureBufferCapacity(len);

            if (this.isMatchingLayout(format, stride)) {
                // The layout is the same, so we can just perform a memory copy
                copyFast(src, dst, stride, count);
            } else {
                // The layout differs, so we need to perform a conversion on the vertex data
                copySlow(src, stride, format, dst, this.dstStride, this.dstFormat, count);
            }
        }

        this.vertexCount += count;
        this.elementOffset += len;
    }

    private boolean isMatchingLayout(VertexFormatDescription format, int stride) {
        return this.dstFormat == format && this.dstStride == stride;
    }

    private static void copyFast(long src, long dst, int stride, int count) {
        MemoryUtil.memCopy(src, dst, stride * count);
    }

    private static void copySlow(long srcBuffer, int srcStride, VertexFormatDescription srcFormat,
                                 long dstBuffer, int dstStride, VertexFormatDescription dstFormat,
                                 int count)
    {
        VertexSerializerCache.get(srcFormat, dstFormat)
                .serialize(srcBuffer, srcStride, dstBuffer, dstStride, count);
    }

    private void ensureBufferCapacity(int additionalBytes) {
        // Ensure that there is always space for 1 more vertex; see BufferBuilder.next()
        additionalBytes += this.dstStride;

        if (this.elementOffset + additionalBytes > this.buffer.capacity()) {
            this.growBuffer(additionalBytes);
        }
    }

    private void growBuffer(int additionalBytes) {
        int size = this.buffer.capacity() + roundBufferSize(additionalBytes);

        LOGGER.debug("Needed to grow BufferBuilder buffer: Old size {} bytes, new size {} bytes.", this.buffer.capacity(), size);

        ByteBuffer byteBuffer = GlAllocationUtils.resizeByteBuffer(this.buffer, size);
        byteBuffer.rewind();

        this.buffer = byteBuffer;
    }
}
