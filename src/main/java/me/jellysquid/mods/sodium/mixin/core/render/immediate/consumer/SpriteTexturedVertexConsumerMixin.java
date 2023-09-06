package me.jellysquid.mods.sodium.mixin.core.render.immediate.consumer;

import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.render.SpriteTexturedVertexConsumer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.Sprite;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteTexturedVertexConsumer.class)
public class SpriteTexturedVertexConsumerMixin implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Unique
    private boolean isFullWriter;

    @Unique
    private float minU, minV;

    @Unique
    private float maxU, maxV;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(VertexConsumer delegate, Sprite sprite, CallbackInfo ci) {
        this.minU = sprite.getMinU();
        this.minV = sprite.getMinV();

        this.maxU = sprite.getMaxU();
        this.maxV = sprite.getMaxV();

        this.isFullWriter = VertexBufferWriter.tryOf(this.delegate) != null;
    }

    @Override
    public boolean isFullWriter() {
        return this.isFullWriter;
    }

    @Override
    public void push(MemoryStack stack, final long ptr, int count, VertexFormatDescription format) {
        transform(ptr, count, format,
                this.minU, this.minV, this.maxU, this.maxV);

        VertexBufferWriter.of(this.delegate)
                .push(stack, ptr, count, format);
    }

    /**
     * Transforms the texture UVs for each vertex from their absolute coordinates into the sprite area specified
     * by the parameters.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param minU   The minimum X-coordinate of the sprite bounds
     * @param minV   The minimum Y-coordinate of the sprite bounds
     * @param maxU   The maximum X-coordinate of the sprite bounds
     * @param maxV   The maximum Y-coordinate of the sprite bounds
     */
    @Unique
    private static void transform(long ptr, int count, VertexFormatDescription format,
                                  float minU, float minV, float maxU, float maxV) {
        long stride = format.stride();
        long offsetUV = format.getElementOffset(CommonVertexAttribute.TEXTURE);

        // The width/height of the sprite
        float w = maxU - minU;
        float h = maxV - minV;

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            // The texture coordinates relative to the sprite bounds
            float u = TextureAttribute.getU(ptr + offsetUV);
            float v = TextureAttribute.getV(ptr + offsetUV);

            // The texture coordinates in absolute space on the sprite sheet
            float ut = minU + (w * u);
            float vt = minV + (h * v);

            TextureAttribute.put(ptr + offsetUV, ut, vt);

            ptr += stride;
        }
    }
}
