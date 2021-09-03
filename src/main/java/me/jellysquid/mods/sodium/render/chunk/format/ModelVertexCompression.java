package me.jellysquid.mods.sodium.render.chunk.format;

public class ModelVertexCompression {
    private static final int POSITION_MAX_VALUE = 32768;
    private static final int TEXTURE_MAX_VALUE = 32768;

    private static final float MODEL_ORIGIN = 8.0f;
    private static final float MODEL_RANGE = 32.0f;
    private static final float MODEL_SCALE = MODEL_RANGE / POSITION_MAX_VALUE;

    private static final float MODEL_SCALE_INV = POSITION_MAX_VALUE / MODEL_RANGE;

    private static final float TEXTURE_SCALE = (1.0f / TEXTURE_MAX_VALUE);

    public static long encodePositionAttribute(float x, float y, float z) {
        return encodePosition(x) | encodePosition(y) << 16 | encodePosition(z) << 32;
    }

    public static int encodeTextureAttribute(float x, float y) {
        return encodeTexture(x) | encodeTexture(y) << 16;
    }

    /**
     * @return The scale to be applied to texture coordinates
     */
    public static float getTextureScale() {
        return TEXTURE_SCALE;
    }

    /**
     * @return The scale to be applied to vertex coordinates
     */
    public static float getModelScale() {
        return MODEL_SCALE;
    }

    /**
     * @return The translation to be applied to vertex coordinates
     */
    public static float getModelOffset() {
        return -MODEL_ORIGIN;
    }

    private static short encodeTexture(float value) {
        return (short) (value * TEXTURE_MAX_VALUE);
    }

    private static long encodePosition(float v) {
        return (short) ((MODEL_ORIGIN + v) * MODEL_SCALE_INV);
    }

    public static int encodeLightMapTexCoord(int light) {
        int r = light;

        // Mask off coordinate values outside 0..255
        r &= 0x00FF_00FF;

        // Light coordinates are normalized values, so upcasting requires a shift
        // Scale the coordinates from the range of 0..255 (unsigned byte) into 0..65535 (unsigned short)
        r <<= 8;

        // Add a half-texel offset to each coordinate so we sample from the center of each texel
        r += 0x0800_0800;

        return r;
    }
}
