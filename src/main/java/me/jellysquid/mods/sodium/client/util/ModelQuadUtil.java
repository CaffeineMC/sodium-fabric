package me.jellysquid.mods.sodium.client.util;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

/**
 * Provides some utilities and constants for interacting with vanilla's model quad vertex format.
 *
 * This is the current vertex format used by Minecraft for chunk meshes and model quads. Internally, it uses integer
 * arrays for store baked quad data, and as such the following table provides both the byte and int indices.
 *
 * Byte Index    Integer Index             Name                 Format                 Fields
 * 0 ..11        0..2                      Position             3 floats               x, y, z
 * 12..15        3                         Color                4 unsigned bytes       a, r, g, b
 * 16..23        4..5                      Block Texture        2 floats               u, v
 * 24..27        6                         Light Texture        2 shorts               u, v
 * 28..30        7                         Normal               3 unsigned bytes       x, y, z
 * 31                                      Padding              1 byte
 */
public class ModelQuadUtil {
    // Integer indices for vertex attributes, useful for accessing baked quad data
    public static final int POSITION_INDEX = 0,
            COLOR_INDEX = 3,
            TEXTURE_INDEX = 4,
            LIGHT_INDEX = 6,
            NORMAL_INDEX = 7;

    // Size of vertex format in 4-byte integers
    public static final int VERTEX_SIZE = 8;

    /**
     * @param vertexIndex The index of the vertex to access
     * @return The starting offset of the vertex's attributes
     */
    public static int vertexOffset(int vertexIndex) {
        return vertexIndex * VERTEX_SIZE;
    }

    public static ModelQuadFacing findNormalFace(float x, float y, float z) {
        Vector3f normal = new Vector3f(x, y, z);

        if (!normal.isFinite()) {
            return ModelQuadFacing.UNASSIGNED;
        }

        float maxDot = 0;
        Direction closestFace = null;

        for (Direction face : DirectionUtil.ALL_DIRECTIONS) {
            float dot = normal.dot(face.getUnitVector());

            if (dot > maxDot) {
                maxDot = dot;
                closestFace = face;
            }
        }

        if (closestFace != null && MathHelper.approximatelyEquals(maxDot, 1.0f)) {
            return ModelQuadFacing.fromDirection(closestFace);
        }

        return ModelQuadFacing.UNASSIGNED;
    }

    public static ModelQuadFacing findNormalFace(int normal) {
        return findNormalFace(NormI8.unpackX(normal), NormI8.unpackY(normal), NormI8.unpackZ(normal));
    }
}
