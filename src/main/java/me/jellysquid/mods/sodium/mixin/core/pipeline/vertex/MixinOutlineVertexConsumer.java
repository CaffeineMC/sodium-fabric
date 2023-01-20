package me.jellysquid.mods.sodium.mixin.core.pipeline.vertex;

import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.vertex.VertexFormatDescription;
import net.minecraft.client.render.FixedColorVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
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
    public void push(long ptr, int count, VertexFormatDescription format) {
        this.writeVerticesSlow(ptr, count, format);
    }

    private void writeVerticesSlow(long ptr, int count, VertexFormatDescription format) {
        var offsetPosition = format.getOffset(VertexFormats.POSITION_ELEMENT);
        var offsetTexture = format.getOffset(VertexFormats.TEXTURE_ELEMENT);

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            float positionX = MemoryUtil.memGetFloat(ptr + offsetPosition + 0);
            float positionY = MemoryUtil.memGetFloat(ptr + offsetPosition + 4);
            float positionZ = MemoryUtil.memGetFloat(ptr + offsetPosition + 8);

            float textureU = MemoryUtil.memGetFloat(ptr + offsetTexture + 0);
            float textureV = MemoryUtil.memGetFloat(ptr + offsetTexture + 4);

            this.delegate.vertex(positionX, positionY, positionZ)
                    .color(this.fixedRed, this.fixedGreen, this.fixedBlue, this.fixedAlpha)
                    .texture(textureU, textureV)
                    .next();

            ptr += format.stride;
        }
    }
}