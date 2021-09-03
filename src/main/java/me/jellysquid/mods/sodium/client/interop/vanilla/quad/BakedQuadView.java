package me.jellysquid.mods.sodium.client.interop.vanilla.quad;

import net.minecraft.client.texture.Sprite;

/**
 * Provides a read-only view of a {@link net.minecraft.client.render.model.BakedQuad}.
 */
public interface BakedQuadView {
    /**
     * @return The x-position of the vertex at index {@param idx}
     */
    float getX(int idx);

    /**
     * @return The y-position of the vertex at index {@param idx}
     */
    float getY(int idx);

    /**
     * @return The z-position of the vertex at index {@param idx}
     */
    float getZ(int idx);

    /**
     * @return The integer-encoded color of the vertex at index {@param idx}
     */
    int getColor(int idx);

    /**
     * @return The texture x-coordinate for the vertex at index {@param idx}
     */
    float getTexU(int idx);

    /**
     * @return The texture y-coordinate for the vertex at index {@param idx}
     */
    float getTexV(int idx);

    /**
     * @return The lightmap texture coordinates for the vertex at index {@param idx}
     */
    int getLight(int idx);

    /**
     * @return The integer-encoded normal vector for the vertex at index {@param idx}
     */
    int getNormal(int idx);

    /**
     * @return The color index of this quad.
     */
    int getColorIndex();

    /**
     * @return The sprite texture used by this quad, or null if none is attached
     */
    Sprite getSprite();
}
