package net.caffeinemc.mods.sodium.client.model.quad;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

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
     * @return The color index of this quad.
     */
    int getColorIndex();

    /**
     * @return The sprite texture used by this quad, or null if none is attached
     */
    TextureAtlasSprite getSprite();
    
    /**
     * @return The face used by this quad for lighting effects
     */
    Direction getLightFace();

    default boolean hasColor() {
        return this.getColorIndex() != -1;
    }
}
