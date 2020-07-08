package me.jellysquid.mods.sodium.client.model.quad;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.minecraft.client.texture.Sprite;

import java.nio.ByteBuffer;

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
     * Copies this quad's data into the specified buffer starting at the given position.
     * @param buf The buffer to write this quad's data to
     * @param position The starting byte index to write to
     */
    default void copyInto(ByteBuffer buf, int position) {
        for (int i = 0; i < 4; i++) {
            buf.putFloat(position, this.getX(i));
            buf.putFloat(position + 4, this.getY(i));
            buf.putFloat(position + 8, this.getZ(i));
            buf.putInt(position + 12, this.getColor(i));
            buf.putFloat(position + 16, this.getTexU(i));
            buf.putFloat(position + 20, this.getTexV(i));
            buf.putInt(position + 24, this.getLight(i));

            position += 28;
        }
    }

    /**
     * @return The sprite texture used by this quad, or null if none is attached
     */
    Sprite getSprite();
}
