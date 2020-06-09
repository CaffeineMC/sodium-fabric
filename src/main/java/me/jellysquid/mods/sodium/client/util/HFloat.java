package me.jellysquid.mods.sodium.client.util;

/**
 * Ported from the original C++ code found in OpenGL Mathematics library.
 * https://github.com/g-truc/glm/blob/master/glm/detail/type_half.inl
 * (MIT License)
 */
public final class HFloat {
    public static short convertFloatToHFloat(final float f) {
        return convertFloatBitsToHFloat(Float.floatToRawIntBits(f));
    }

    public static short convertFloatBitsToHFloat(final int floatBits) {
        final int s = (floatBits >> 16) & 0x00008000;

        int e = ((floatBits >> 23) & 0x000000ff) - (127 - 15);
        int m = floatBits & 0x007fffff;

        // Now reassemble s, e and m into a half:
        if (e <= 0) {
            if (e < -10) {
                // E is less than -10. The absolute value of f is less than HALF_MIN (f may be a small normalized float, a denormalized float or a zero).
                // We convert f to a half zero with the same sign as f.

                return (short) s;
            }

            // E is between -10 and 0. F is a normalized float
            // whose magnitude is less than HALF_NRM_MIN.
            //
            // We convert f to a denormalized half.
            // Add an explicit leading 1 to the significand.
            m |= 0x00800000;

            // Round to m to the nearest (10+e)-bit value (with e between
            // -10 and 0); in case of a tie, round to the nearest even value.
            //
            // Rounding may cause the significand to overflow and make
            // our number normalized. Because of the way a half's bits
            // are laid out, we don't have to treat this case separately;
            // the code below will handle it correctly.
            final int t = 14 - e;
            final int a = (1 << (t - 1)) - 1;
            final int b = (m >> t) & 1;

            m = (m + a + b) >> t;

            // Assemble the half from s, e (zero) and m.
            final int r = s | m;

            return (short) r;
        } else if (e == 0xff - (127 - 15)) {
            if (m == 0) {
                // F is an infinity; convert f to a half infinity with the same sign as f.
                return (short) (s | 0x7c00);
            } else {
                // F is a NAN; we produce a half NAN that preserves
                // the sign bit and the 10 leftmost bits of the
                // significand of f, with one exception: If the 10
                // leftmost bits are all zero, the NAN would turn
                // into an infinity, so we have to set at least one
                // bit in the significand.
                m >>= 13;

                return (short) (s | 0x7c00 | m | ((m == 0) ? 1 : 0));
            }
        } else {
            // E is greater than zero. F is a normalized float.
            // We try to convert f to a normalized half.
            //
            // Round to m to the nearest 10-bit value. In case of
            // a tie, round to the nearest even value.
            m += 0x00000fff + ((m >> 13) & 1);

            if ((m & 0x00800000) != 0) {
                m = 0;                     // overflow in significand,
                e += 1;                    // adjust exponent
            }

            //Handle exponent overflow
            if (e > 30) {
                overflow();                // Cause a hardware floating point overflow;
                                           // if this returns, the half becomes an infinity with the same sign as f.
                return (short) (s | 0x7c00);
            }

            // Assemble the half from s, e and m.
            return (short) (s | (e << 10) | (m >> 13));
        }
    }

    private static void overflow() {
        float f = 1.0e10f;

        for (int i = 0; i < 10; i++) {
            f *= f;  // This will overflow before the for loop terminates
        }
    }
}