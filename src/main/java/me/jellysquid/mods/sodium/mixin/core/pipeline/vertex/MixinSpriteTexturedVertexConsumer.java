package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpriteTexturedVertexConsumer.class)
public class MixinSpriteTexturedVertexConsumer implements VertexBufferWriter  {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Shadow
    @Final
    private Sprite sprite;

    @Override
    public void push(final long ptr, int vertexCount, int stride, VertexFormatDescription format) {
        this.transformVertices(ptr, vertexCount, stride, format);

        VertexBufferWriter.of(this.delegate)
                .push(ptr, vertexCount, stride, format);
    }

    @Override
    public long buffer(MemoryStack stack, int count, int stride, VertexFormatDescription format) {
        return VertexBufferWriter.of(this.delegate)
                .buffer(stack, count, stride, format);
    }

    private void transformVertices(final long ptr, int count, int stride, VertexFormatDescription format) {
        long offset = ptr;
        long offsetUV = format.getOffset(VertexFormats.TEXTURE_ELEMENT);

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            float u = MemoryUtil.memGetFloat(offset + offsetUV + 0);
            float v = MemoryUtil.memGetFloat(offset + offsetUV + 4);

            u = this.sprite.getFrameU(u * 16.0f);
            v = this.sprite.getFrameV(v * 16.0f);

            MemoryUtil.memPutFloat(offset + offsetUV + 0, u);
            MemoryUtil.memPutFloat(offset + offsetUV + 4, v);

            offset += stride;
        }
    }
}
