package me.jellysquid.mods.sodium.client.render.chunk.format;

public class ModelVertexUtil {
    /**
     * Converts a floating point vertex position in range 0..32 to a de-normalized unsigned short.
     * @param value The float in range 0..32
     * @return The resulting de-normalized unsigned short
     */
    public static short denormalizeVertexPositionFloatAsShort(float value) {
        // Since we're skipping the scaling formerly done in ChunkModelVertexTransformer to preserve precision, this
        // method actually takes input unnormalized within the range 0..32, and expands that to 0..65536.
        // TODO: Restructure things to be less arbitrary here
        return (short) (value * 2048.0f);
    }

    /**
     * Converts a normalized floating point texture coordinate to a de-normalized unsigned short.
     * @param value The normalized float
     * @return The resulting de-normalized unsigned short
     */
    public static short denormalizeVertexTextureFloatAsShort(float value) {
        return (short) (value * 32768.0f);
    }

    /**
     * This moves some work out the shader code and simplifies things a bit. In vanilla, the game encodes light map
     * texture coordinates as two un-normalized unsigned shorts in the range 0..255. Using the fixed-function pipeline,
     * it then applies a matrix transformation which normalizes these coordinates and applies a centering offset. This
     * operation has non-zero overhead and complicates shader code a bit.
     *
     * To work around the problem, this function instead normalizes these light map texture coordinates and applies the
     * centering offset, allowing it to be baked into the vertex data itself.
     *
     * @param light The light map value
     * @return The light map texture coordinates as two unsigned shorts with a center offset applied
     */
    public static int encodeLightMapTexCoord(int light) {
        int sl = (light >> 16) & 255;
        sl = (sl << 8) + 2048;

        int bl = light & 255;
        bl = (bl << 8) + 2048;

        return (sl << 16) | bl;
    }
}
