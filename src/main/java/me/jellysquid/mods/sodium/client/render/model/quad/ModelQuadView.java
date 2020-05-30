package me.jellysquid.mods.sodium.client.render.model.quad;

import net.minecraft.client.texture.Sprite;

/**
 * Provides a read-only view of a model quad. For mutable access to a model quad, see {@link ModelQuadViewMutable}.
 */
public interface ModelQuadView {
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
     * @return The integer bit flags containing the {@link ModelQuadFlags} for this quad
     */
    int getFlags();

    /**
     * @return The lightmap texture coordinates for the vertex at index {@param idx}
     */
    int getLight(int idx);

    /**
     * @return The integer-encoded normal vector for the vertex at index {@param idx}
     */
    int getNormal(int idx);

    /**
     * This method is a temporary stop-gap in order to facilitate fast memory copies between vertex arrays.
     * TODO: Replace with a generic method which can copy data into a target array without exposing mutable access
     * @return The backing vertex data array for this quad
     */
    @Deprecated
    int[] getVertexData();

    /**
     * @return The sprite texture used by this quad, or null if none is attached
     */
    Sprite getSprite();
}
