package me.jellysquid.mods.sodium.client.util.color;

public interface ColorU8 {
    /**
     * The maximum value of a color component.
     */
    float COMPONENT_RANGE = 255.0f;

    /**
     * Constant value which can be multiplied with a floating-point color component to get the normalized value. The
     * multiplication is slightly faster than a floating point division, and this code is a hot path which justifies it.
     */
    float NORM = 1.0f / COMPONENT_RANGE;

    /**
     * Normalizes a color component to the range of 0..1.
     */
    static float normalize(float v) {
        return v * NORM;
    }
}
