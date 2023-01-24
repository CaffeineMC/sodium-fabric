package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/client/render/OutlineVertexConsumerProvider$OutlineVertexConsumer")
public abstract class MixinOutlineVertexConsumer extends FixedColorVertexConsumer implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Override
    public void push(long ptr, int count, int stride, VertexFormatDescription format) {
        this.transformVertices(ptr, count, stride, format);
        VertexBufferWriter.of(this.delegate)
                .push(ptr, count, stride, format);
    }

    @Override
    public long buffer(MemoryStack stack, int count, int stride, VertexFormatDescription format) {
        return VertexBufferWriter.of(this.delegate)
                .buffer(stack, count, stride, format);
    }

    private void transformVertices(long ptr, int count, int stride, VertexFormatDescription format) {
        long offset = ptr;
        var offsetColor = format.getOffset(VertexFormats.COLOR_ELEMENT);
        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            MemoryUtil.memPutInt(offset + offsetColor, ColorABGR.pack(this.fixedRed, this.fixedGreen, this.fixedBlue, this.fixedAlpha));
            offset += stride;
        }
    }
}