package me.jellysquid.mods.sodium.client.render.model.quad;

import net.minecraft.client.texture.Sprite;

public interface ModelQuadView {
    float getX(int idx);

    float getY(int idx);

    float getZ(int idx);

    int getColor(int idx);

    float getTexU(int idx);

    float getTexV(int idx);

    int getFlags();

    int[] getVertexData();

    Sprite getSprite();
}
