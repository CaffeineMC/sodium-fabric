package net.caffeinemc.mods.sodium.mixin.core.render.immediate.consumer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.caffeinemc.mods.sodium.api.memory.MemoryIntrinsics;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.serializer.VertexSerializerRegistry;
import net.caffeinemc.mods.sodium.client.render.vertex.buffer.BufferBuilderExtension;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexBufferWriter, BufferBuilderExtension {
    @Shadow
    private int vertices;

    @Shadow
    @Final
    private int vertexSize;

    @Shadow
    private long vertexPointer;

    @Shadow
    @Final
    private ByteBufferBuilder buffer;

    @Shadow
    private int elementsToFill;

    @Shadow
    @Final
    private VertexFormat format;

    @Override
    public void sodium$duplicateVertex() {
        if (this.vertices == 0) {
            return;
        }

        long head = this.buffer.reserve(this.vertexSize);
        MemoryIntrinsics.copyMemory(head - this.vertexSize, head, this.vertexSize);

        this.vertices++;
    }

    @Override
    public void push(MemoryStack stack, long src, int count, VertexFormat format) {
        var length = count * this.vertexSize;

        // The buffer may change in the even, so we need to make sure that the
        // pointer is retrieved *after* the resize
        var dst = this.buffer.reserve(length);

        if (format == this.format) {
            // The layout is the same, so we can just perform a memory copy
            // The stride of a vertex format is always 4 bytes, so this aligned copy is always safe
            MemoryIntrinsics.copyMemory(src, dst, length);
        } else {
            // The layout differs, so we need to perform a conversion on the vertex data
            this.copySlow(src, dst, count, format);
        }

        this.vertices += count;
        this.vertexPointer = (dst + length) - vertexSize;
        this.elementsToFill = 0;
    }

    @Unique
    private void copySlow(long src, long dst, int count, VertexFormat format) {
        VertexSerializerRegistry.instance()
                .get(format, this.format)
                .serialize(src, dst, count);
    }
}
