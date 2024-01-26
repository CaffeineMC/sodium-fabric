package me.jellysquid.mods.sodium.mixin.core.render.immediate.consumer;

import me.jellysquid.mods.sodium.client.render.vertex.buffer.ExtendedBufferBuilder;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder;
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
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin extends FixedColorVertexConsumer implements VertexBufferWriter, ExtendedBufferBuilder {
    @Shadow
    protected abstract void grow(int size);

    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private int vertexCount;

    @Shadow
    private int elementOffset;

    @Shadow
    private VertexFormat.DrawMode drawMode;

    @Unique
    private VertexFormatDescription formatDescription;

    @Unique
    private int vertexStride;

    private SodiumBufferBuilder fastDelegate;

    @Inject(
            method = "setFormat",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/render/BufferBuilder;format:Lnet/minecraft/client/render/VertexFormat;",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void onFormatChanged(VertexFormat format, CallbackInfo ci) {
        this.formatDescription = VertexFormatRegistry.instance()
                .get(format);
        this.vertexStride = this.formatDescription.stride();
        this.fastDelegate = this.formatDescription.isSimpleFormat() ? new SodiumBufferBuilder(this) : null;
    }

    @Inject(method = { "reset", "resetBuilding", "begin" }, at = @At("RETURN"))
    private void resetDelegate(CallbackInfo ci) {
        if (this.fastDelegate != null) {
            this.fastDelegate.reset();
        }
    }

    @Override
    public ByteBuffer sodium$getBuffer() {
        return this.buffer;
    }

    @Override
    public int sodium$getElementOffset() {
        return this.elementOffset;
    }

    @Override
    public VertexFormatDescription sodium$getFormatDescription() {
        return this.formatDescription;
    }

    @Override
    public SodiumBufferBuilder sodium$getDelegate() {
        return this.fastDelegate;
    }

    @Override
    public void sodium$moveToNextVertex() {
        this.vertexCount++;
        this.elementOffset += this.vertexStride;

        this.grow(this.vertexStride);

        if (this.shouldDuplicateVertices()) {
            this.duplicateVertex();
        }
    }

    @Override
    public boolean sodium$usingFixedColor() {
        return this.colorFixed;
    }

    @Unique
    private boolean shouldDuplicateVertices() {
        return this.drawMode == VertexFormat.DrawMode.LINES || this.drawMode == VertexFormat.DrawMode.LINE_STRIP;
    }

    @Unique
    private void duplicateVertex() {
        MemoryIntrinsics.copyMemory(
                MemoryUtil.memAddress(this.buffer, this.elementOffset - this.vertexStride),
                MemoryUtil.memAddress(this.buffer, this.elementOffset),
                this.vertexStride);

        this.elementOffset += this.vertexStride;
        this.vertexCount++;

        this.grow(this.vertexStride);
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.formatDescription != null && this.formatDescription.isSimpleFormat();
    }

    @Override
    public void push(MemoryStack stack, long src, int count, VertexFormatDescription format) {
        var length = count * this.vertexStride;

        // Ensure that there is always space for 1 more vertex; see BufferBuilder.next()
        this.grow(length + this.vertexStride);

        // The buffer may change in the even, so we need to make sure that the
        // pointer is retrieved *after* the resize
        var dst = MemoryUtil.memAddress(this.buffer, this.elementOffset);

        if (format == this.formatDescription) {
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
                .get(format, this.formatDescription)
                .serialize(src, dst, count);
    }
}
