package me.jellysquid.mods.sodium.mixin.core.render.immediate.consumer;

import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializerRegistry;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexBufferWriter {
    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private int vertexCount;

    @Shadow
    private int elementOffset;

    @Shadow
    protected abstract void grow(int size);

    @Unique
    private VertexFormatDescription format;

    @Unique
    private int stride;

    @Inject(method = "setFormat",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/render/BufferBuilder;format:Lnet/minecraft/client/render/VertexFormat;",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void onFormatChanged(VertexFormat format, CallbackInfo ci) {
        this.format = VertexFormatRegistry.instance()
                .get(format);
        this.stride = format.getVertexSizeByte();
    }

    @Override
    public boolean isFullWriter() {
        return true;
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
            MemoryIntrinsics.copyMemory(src, dst, length);
        } else {
            // The layout differs, so we need to perform a conversion on the vertex data
            this.copySlow(src, dst, count, format);
        }

        this.vertexCount += count;
        this.elementOffset += length;
    }

    @Unique
    private void copySlow(long src, long dst, int count, VertexFormatDescription format) {
        VertexSerializerRegistry.instance()
                .get(format, this.format)
                .serialize(src, dst, count);
    }
}
