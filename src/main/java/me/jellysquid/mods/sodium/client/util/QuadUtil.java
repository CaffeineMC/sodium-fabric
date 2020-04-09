package me.jellysquid.mods.sodium.client.util;

import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

public class QuadUtil {
    public static final int POSITION_INDEX = 0,
            COLOR_INDEX = 3,
            TEXTURE_INDEX = 4,
            LIGHT_INDEX = 6,
            NORMAL_INDEX = 7;

    public static final int VERTEX_SIZE = 8;

    private static final int[] NORMALS = new int[DirectionUtil.ALL_DIRECTIONS.length];
    private static final float NORM = 1.0f / 127.0f;

    static {
        for (int i = 0; i < NORMALS.length; i++) {
            NORMALS[i] = QuadUtil.encodeNormal(DirectionUtil.ALL_DIRECTIONS[i].getVector());
        }
    }

    public static int vertexOffset(int vertexIndex) {
        return vertexIndex * VERTEX_SIZE;
    }

    private static int encodeNormal(Vec3i norm) {
        return encodeNormal(norm.getX(), norm.getY(), norm.getZ());
    }

    public static int encodeNormal(Vector3f dir) {
        return encodeNormal(dir.getX(), dir.getY(), dir.getZ());
    }

    public static int encodeNormal(float x, float y, float z) {
        int normX = encodeNormal(x);
        int normY = encodeNormal(y);
        int normZ = encodeNormal(z);

        return (normZ << 16) | (normY << 8) | normX;
    }

    private static int encodeNormal(float v) {
        return ((int) (MathHelper.clamp(v, -1.0F, 1.0F) * 127.0F) & 255);
    }

    public static int getNormal(Direction facing) {
        return NORMALS[facing.ordinal()];
    }

    public static float unpackNormalX(int norm) {
        return ((byte) (norm & 0xFF)) * NORM;
    }

    public static float unpackNormalY(int norm) {
        return ((byte) ((norm >> 8) & 0xFF)) * NORM;
    }

    public static float unpackNormalZ(int norm) {
        return ((byte) ((norm >> 16) & 0xFF)) * NORM;
    }
}
