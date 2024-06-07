package me.jellysquid.mods.sodium.mixin.core.render.immediate.consumer;

import me.jellysquid.mods.sodium.client.render.vertex.buffer.ExtendedBufferBuilder;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.*;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializerRegistry;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexBufferWriter, ExtendedBufferBuilder {
    @Shadow
    @Final
    private int vertexSizeByte;

    @Shadow
    @Final
    private BufferAllocator allocator;

    @Shadow
    private int vertexCount;

    @Shadow
    private long vertexPointer;

    @Unique
    private VertexFormatDescription formatDescription;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void onFormatChanged(BufferAllocator allocator, VertexFormat.DrawMode drawMode, VertexFormat format, CallbackInfo ci) {
        this.formatDescription = VertexFormatRegistry.instance()
                .get(format);
    }

    @Override
    public void sodium$duplicatePreviousVertex() {
        if (this.vertexCount != 0) {
            long dst = this.allocator.allocate(this.vertexSizeByte);
            MemoryIntrinsics.copyMemory(dst - this.vertexSizeByte, dst, this.vertexSizeByte);
            ++this.vertexCount;
        }
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.formatDescription != null && this.formatDescription.isSimpleFormat();
    }

    @Override
    public void push(MemoryStack stack, long src, int count, VertexFormatDescription format) {
        var length = count * this.vertexSizeByte;

        var dst = this.allocator.allocate(length);

        if (format == this.formatDescription) {
            // The layout is the same, so we can just perform a memory copy
            // The stride of a vertex format is always 4 bytes, so this aligned copy is always safe
            MemoryIntrinsics.copyMemory(src, dst, length);
        } else {
            // The layout differs, so we need to perform a conversion on the vertex data
            this.copySlow(src, dst, count, format);
        }

        this.vertexCount += count;
        this.vertexPointer = dst + length;
    }

    @Unique
    private void copySlow(long src, long dst, int count, VertexFormatDescription format) {
        VertexSerializerRegistry.instance()
                .get(format, this.formatDescription)
                .serialize(src, dst, count);
    }
}
