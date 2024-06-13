package net.caffeinemc.mods.sodium.mixin.core.render.immediate.consumer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.vertex.buffer.BufferBuilderExtension;
import net.caffeinemc.mods.sodium.client.render.vertex.buffer.DirectBufferBuilder;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializerRegistry;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexBufferWriter, BufferBuilderExtension {
    @Shadow
    private int vertices;

    @Shadow
    private VertexFormat.Mode mode;

    @Shadow
    @Final
    private int vertexSize;
    @Shadow
    private long vertexPointer;
    @Shadow
    @Final
    private ByteBufferBuilder buffer;
    @Unique
    private VertexFormatDescription formatDescription;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onFormatChanged(ByteBufferBuilder byteBufferBuilder, VertexFormat.Mode mode, VertexFormat format, CallbackInfo ci) {
        this.formatDescription = VertexFormatRegistry.instance()
                .get(format);
    }

    @Override
    public void sodium$duplicateVertex() {
        if (vertices == 0) return;

        // TODO IMS
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.formatDescription != null && this.formatDescription.isSimpleFormat();
    }

    @Override
    public void push(MemoryStack stack, long src, int count, VertexFormatDescription format) {
        var length = count * this.vertexSize;

        // The buffer may change in the even, so we need to make sure that the
        // pointer is retrieved *after* the resize
        var dst = this.buffer.reserve(length);

        if (format == this.formatDescription) {
            // The layout is the same, so we can just perform a memory copy
            // The stride of a vertex format is always 4 bytes, so this aligned copy is always safe
            MemoryIntrinsics.copyMemory(src, dst, length);
        } else {
            // The layout differs, so we need to perform a conversion on the vertex data
            this.copySlow(src, dst, count, format);
        }

        this.vertices += count;
        this.vertexPointer = dst + length;
    }

    @Unique
    private void copySlow(long src, long dst, int count, VertexFormatDescription format) {
        VertexSerializerRegistry.instance()
                .get(format, this.formatDescription)
                .serialize(src, dst, count);
    }
}
