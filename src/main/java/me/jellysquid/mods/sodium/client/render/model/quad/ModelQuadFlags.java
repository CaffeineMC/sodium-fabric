package me.jellysquid.mods.sodium.client.render.model.quad;

import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;

public class ModelQuadFlags {
    public static final int IS_ALIGNED = 0b01;
    public static final int IS_PARTIAL = 0b10;
    public static final int NONE = 0;

    public static boolean contains(int flags, int mask) {
        return (flags & mask) != 0;
    }

    public static int getQuadFlags(BakedQuad bakedQuad) {
        ModelQuadView quad = (ModelQuadView) bakedQuad;
        Direction face = bakedQuad.getFace();

        float minX = 32.0F;
        float minY = 32.0F;
        float minZ = 32.0F;

        float maxX = -32.0F;
        float maxY = -32.0F;
        float maxZ = -32.0F;

        for (int i = 0; i < 4; ++i) {
            float x = quad.getX(i);
            float y = quad.getY(i);
            float z = quad.getZ(i);

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        boolean f1 = false, f0 = false;

        switch (face) {
            case DOWN:
                f0 = minY == maxY && minY < 1.0E-4F;
                break;
            case UP:
                f0 = minY == maxY && maxY > 0.9999F;
                break;
            case NORTH:
                f0 = minZ == maxZ && minZ < 1.0E-4F;
                break;
            case SOUTH:
                f0 = minZ == maxZ && maxZ > 0.9999F;
                break;
            case WEST:
                f0 = minX == maxX && minX < 1.0E-4F;
                break;
            case EAST:
                f0 = minX == maxX && maxX > 0.9999F;
                break;
        }

        switch (face.getAxis()) {
            case X:
                f1 = minY >= 1.0E-4F || minZ >= 1.0E-4F || maxY <= 0.9999F || maxZ <= 0.9999F;
                break;
            case Y:
                f1 = minX >= 1.0E-4F || minZ >= 1.0E-4F || maxX <= 0.9999F || maxZ <= 0.9999F;
                break;
            case Z:
                f1 = minX >= 1.0E-4F || minY >= 1.0E-4F || maxX <= 0.9999F || maxY <= 0.9999F;
                break;
        }

        int flags = 0;

        if (f1) {
            flags |= IS_PARTIAL;
        }

        if (f0) {
            flags |= IS_ALIGNED;
        }

        return flags;
    }
}
