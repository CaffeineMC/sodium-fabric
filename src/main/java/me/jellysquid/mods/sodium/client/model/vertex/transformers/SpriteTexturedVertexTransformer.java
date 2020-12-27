package me.jellysquid.mods.sodium.client.model.vertex.transformers;

import net.minecraft.client.texture.Sprite;

public class SpriteTexturedVertexTransformer implements VertexTransformer {
    private final float uMin;
    private final float vMin;

    private final float uMaxMin;
    private final float vMaxMin;

    public SpriteTexturedVertexTransformer(Sprite sprite) {
        this.uMin = sprite.getMinU();
        this.vMin = sprite.getMinV();

        this.uMaxMin = sprite.getMaxU() - this.uMin;
        this.vMaxMin = sprite.getMaxV() - this.vMin;
    }

    @Override
    public float transformTextureU(float u) {
        return (this.uMaxMin * u) + this.uMin;
    }

    @Override
    public float transformTextureV(float v) {
        return (this.vMaxMin * v) + this.vMin;
    }
}
