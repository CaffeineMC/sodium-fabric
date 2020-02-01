package me.jellysquid.mods.sodium.client.util;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

public class QuadUtil {
    private static final Direction[] FACES = Direction.values();

    public static final int POSITION_INDEX = 0,
            COLOR_INDEX = 3,
            TEXTURE_INDEX = 4,
            LIGHT_INDEX = 6,
            NORMAL_INDEX = 7;

    public static final int VERTEX_SIZE = 8;

    private static final int[] NORMALS = new int[FACES.length];

    static {
        for (int i = 0; i < NORMALS.length; i++) {
            NORMALS[i] = QuadUtil.encodeNormal(FACES[i].getVector());
        }
    }

    public static int vertexOffset(int vertexIndex) {
        return vertexIndex * VERTEX_SIZE;
    }

    private static int encodeNormal(Vec3i norm) {
        int normX = encodeNormal(norm.getX());
        int normY = encodeNormal(norm.getY());
        int normZ = encodeNormal(norm.getZ());

        return (normX << 16) | (normY << 8) | normZ;
    }

    private static int encodeNormal(float v) {
        return ((int) (MathHelper.clamp(v, -1.0F, 1.0F) * 127.0F) & 255);
    }

    public static int getNormal(Direction facing) {
        return NORMALS[facing.ordinal()];
    }
}
