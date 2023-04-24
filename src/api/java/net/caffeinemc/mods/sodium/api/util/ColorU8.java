package net.caffeinemc.mods.sodium.api.util;

public interface ColorU8 {
    /**
     * The number of bits used for each color component.
     */
    int COMPONENT_BITS = 8;

    /**
     * The bitwise mask for each color component.
     */
    int COMPONENT_MASK = (1 << COMPONENT_BITS) - 1;

    /**
     * The maximum value of a color component. Used for converting normalizing floats to integers.
     */
    float COMPONENT_RANGE = (float) COMPONENT_MASK;

    /**
     * The multiplicative inverse of (value / COMPONENT_RANGE). Used for converting integers to normalized floats.
     */
    float COMPONENT_RANGE_INVERSE = 1.0f / COMPONENT_RANGE;

    /**
     * Converts a normalized float to an integer component.
     * @param value The floating point value in the range of 0.0..1.0
     * @return The integer component of the floating point value in 0..255 range
     */
    static int normalizedFloatToByte(float value) {
        return (int) (value * COMPONENT_RANGE) & COMPONENT_MASK;
    }

    /**
     * Converts an integer component to a normalized floating point value.
     * @param value The integer component in 0..255 range
     * @return The floating point value of the integer component in the range of 0.0..1.0
     */
    static float byteToNormalizedFloat(byte value) {
        return (float) Byte.toUnsignedInt(value) * COMPONENT_RANGE_INVERSE;
    }

    /**
     * Converts an integer component to a normalized floating point value.
     * @param value The integer component in 0..255 range
     * @return The floating point value of the integer component in the range of 0.0..1.0
     */
    static float byteToNormalizedFloat(int value) {
        return (float) value * COMPONENT_RANGE_INVERSE;
    }
}
