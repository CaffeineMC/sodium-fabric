package net.caffeinemc.mods.sodium.api.util;

import org.lwjgl.system.MemoryUtil;

public record RawUVs(float minU, float minV, float maxU, float maxV) {
    public static final int STRIDE = 16;

    public void put(long ptr) {
        MemoryUtil.memPutFloat(ptr + 0, minU);
        MemoryUtil.memPutFloat(ptr + 4, minV);
        MemoryUtil.memPutFloat(ptr + 8, maxU);
        MemoryUtil.memPutFloat(ptr + 12, maxV);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof RawUVs other) {
            return minU == other.minU() &&
                    minV == other.minV() &&
                    maxU == other.maxU() &&
                    maxV == other.maxV();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = 1;

        result = 31 * result + Float.floatToRawIntBits(minU);
        result = 31 * result + Float.floatToRawIntBits(minV);
        result = 31 * result + Float.floatToRawIntBits(maxU);
        result = 31 * result + Float.floatToRawIntBits(maxV);

        return result;
    }
}
