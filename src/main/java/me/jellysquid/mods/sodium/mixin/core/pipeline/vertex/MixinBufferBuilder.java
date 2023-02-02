package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatRegistry;
import me.jellysquid.mods.sodium.client.render.vertex.serializers.VertexSerializerCache;
import me.jellysquid.mods.sodium.common.util.UnsafeUtil;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
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
    protected abstract void grow(int size);

    private VertexFormatDescription format;
    private int stride;

    @Inject(method = "setFormat", at = @At("RETURN"))
    private void onFormatChanged(VertexFormat format, CallbackInfo ci) {
        this.format = VertexFormatRegistry.get(format);
        this.stride = format.getVertexSizeByte();
    }

    @Override
    public void push(MemoryStack stack, long src, int count, VertexFormatDescription format) {
        var length = count * this.stride;

        // Ensure that there is always space for 1 more vertex; see BufferBuilder.next()
        this.grow(length + this.stride);

        // The buffer may change in the even, so we need to make sure that the
        // pointer is retrieved *after* the resize
        var dst = MemoryUtil.memAddress(this.buffer, this.elementOffset);

        if (format == this.format) {
            // The layout is the same, so we can just perform a memory copy
            // The stride of a vertex format is always 4 bytes, so this aligned copy is always safe
            UnsafeUtil.copyMemory(src, dst, length);
        } else {
            // The layout differs, so we need to perform a conversion on the vertex data
            this.copySlow(src, dst, count, format);
        }

        this.vertexCount += count;
        this.elementOffset += length;
    }

    private void copySlow(long src, long dst, int count, VertexFormatDescription format) {
        VertexSerializerCache.get(format, this.format)
                .serialize(src, dst, count);
    }
}
