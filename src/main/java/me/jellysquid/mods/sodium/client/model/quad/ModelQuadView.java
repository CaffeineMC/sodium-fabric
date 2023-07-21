package me.jellysquid.mods.sodium.client.model.quad;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.MathHelper;

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
    Sprite getSprite();

    /**
     * @return The normal vector of the quad in 3-byte packed format
     */
    int getNormal();

    /**
     * @return The x coordinate of the unit normal vector
     */
    int getNormX();

    /**
     * @return The y coordinate of the unit normal vector
     */
    int getNormY();

    /**
     * @return The z coordinate of the unit normal vector
     */
    int getNormZ();

    /**
     * Sets an accurate normal vector for this quad. This is used for GFNI.
     * 
     * @param x The normal's x component
     * @param y The normal's y component
     * @param z The normal's z component
     */
    void setAccurateNormal(int x, int y, int z);

    /**
     * Calculates the normal vector for this quad using the cross product of the
     * vertices. Ensures the normal vector returned by the getNorm methods is up to
     * date (and a unit vector).
     */
    default void calculateAccurateNormal() {
        final float x0 = getX(0);
        final float y0 = getY(0);
        final float z0 = getZ(0);

        final float x1 = getX(1);
        final float y1 = getY(1);
        final float z1 = getZ(1);

        final float x2 = getX(2);
        final float y2 = getY(2);
        final float z2 = getZ(2);

        final float x3 = getX(3);
        final float y3 = getY(3);
        final float z3 = getZ(3);

        final float dx0 = x2 - x0;
        final float dy0 = y2 - y0;
        final float dz0 = z2 - z0;
        final float dx1 = x3 - x1;
        final float dy1 = y3 - y1;
        final float dz1 = z3 - z1;

        float normX = dy0 * dz1 - dz0 * dy1;
        float normY = dz0 * dx1 - dx0 * dz1;
        float normZ = dx0 * dy1 - dy0 * dx1;

        float l = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);

        if (l != 0 && l != 1) {
            normX /= l;
            normY /= l;
            normZ /= l;
        }

        this.setAccurateNormal(
            (int) (normX * 32),
            (int) (normY * 32),
            (int) (normZ * 32));
    }

    default boolean hasColor() {
        return this.getColorIndex() != -1;
    }
}
