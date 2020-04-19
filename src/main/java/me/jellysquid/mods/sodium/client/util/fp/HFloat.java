package me.jellysquid.mods.sodium.client.util.fp;

public class HFloat {
    public static short encodeHalfS(float fval) {
        return (short) encodeHalfBits(Float.floatToIntBits(fval));
    }

    private static int encodeHalfBits(int fbits) {
        int sign = fbits >>> 16 & 0x8000;
        int val = (fbits & 0x7fffffff) + 0x1000;

        if (val >= 0x47800000) {
            if ((fbits & 0x7fffffff) >= 0x47800000) {
                if (val < 0x7f800000) {
                    return sign | 0x7c00;
                }

                return sign | 0x7c00 | (fbits & 0x007fffff) >>> 13;
            }

            return sign | 0x7bff;
        }

        if (val >= 0x38800000) {
            return sign | val - 0x38000000 >>> 13;
        }

        if (val < 0x33000000) {
            return sign;
        }

        val = (fbits & 0x7fffffff) >>> 23;

        return sign | ((fbits & 0x7fffff | 0x800000) + (0x800000 >>> val - 102) >>> 126 - val);
    }
}
