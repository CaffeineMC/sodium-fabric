package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.Sprite;
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
    public void push(final long ptr, int vertexCount, VertexFormatDescription format) {
        this.transformVertices(ptr, vertexCount, format);

        VertexBufferWriter.of(this.delegate)
                .push(ptr, vertexCount, format);
    }

    private void transformVertices(final long ptr, int vertexCount, VertexFormatDescription format) {
        long offset = ptr;
        long offsetUV = format.getOffset(VertexFormats.TEXTURE_ELEMENT);

        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            float u = MemoryUtil.memGetFloat(offset + offsetUV + 0);
            float v = MemoryUtil.memGetFloat(offset + offsetUV + 4);

            u = this.sprite.getFrameU(u * 16.0f);
            v = this.sprite.getFrameV(v * 16.0f);

            MemoryUtil.memPutFloat(offset + offsetUV + 0, u);
            MemoryUtil.memPutFloat(offset + offsetUV + 4, v);

            offset += format.stride;
        }
    }
}
