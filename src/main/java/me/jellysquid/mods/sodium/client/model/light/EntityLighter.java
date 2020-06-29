package me.jellysquid.mods.sodium.client.model.light;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;

public class EntityLighter {
    private static final double MAX_LIGHT_VAL = 15.0;
    private static final double MAX_LIGHTMAP_COORD = 240.0D;

    public static int getBlendedLight(Entity entity, float tickDelta) {
        boolean calcBlockLight = !entity.isOnFire();

        // Find the interpolated position of the entity
        double x1 = MathHelper.lerp(tickDelta, entity.prevX, entity.getX());
        double y1 = MathHelper.lerp(tickDelta, entity.prevY, entity.getY());
        double z1 = MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ());
        double x2 = x1 + entity.getWidth();
        double y2 = y1 + entity.getHeight();
        double z2 = z1 + entity.getWidth();

        // The sampling volume of blocks which could possibly contribute light to this entity
        int bMinX = MathHelper.floor(x1);
        int bMinY = MathHelper.floor(y1);
        int bMinZ = MathHelper.floor(z1);
        int bMaxX = MathHelper.floor(x2);
        int bMaxY = MathHelper.floor(y2);
        int bMaxZ = MathHelper.floor(z2);

        // The maximum amount of light that could be contributed
        double max = 0.0D;

        // The sampled light values contributed by all sources
        double sl = 0;
        double bl = 0;

        BlockPos.Mutable pos = new BlockPos.Mutable();

        // Iterate over every block in the sampling volume
        for (int bX = bMinX; bX <= bMaxX; bX++) {
            for (int bY = bMinY; bY <= bMaxY; bY++) {
                for (int bZ = bMinZ; bZ <= bMaxZ; bZ++) {
                    pos.set(bX, bY, bZ);

                    BlockState blockState = entity.world.getBlockState(pos);

                    // Do not consider light-blocking volumes
                    if (blockState.isOpaqueFullCube(entity.world, pos) && blockState.getLuminance() <= 0) {
                        continue;
                    }

                    // Find the intersecting volume between the entity box and the block's bounding box
                    double ix1 = Math.max(bX, x1);
                    double iy1 = Math.max(bY, y1);
                    double iz1 = Math.max(bZ, z1);
                    double ix2 = Math.min(bX + 1, x2);
                    double iy2 = Math.min(bY + 1, y2);
                    double iz2 = Math.min(bZ + 1, z2);

                    // The amount of light this block can contribute is the volume of the intersecting box
                    double weight = (ix2 - ix1) * (iy2 - iy1) * (iz2 - iz1);

                    // Keep count of how much light could've been contributed
                    max += weight;

                    // Sum the light actually contributed by this volume
                    sl += weight * (entity.world.getLightLevel(LightType.SKY, pos) / MAX_LIGHT_VAL);

                    if (calcBlockLight) {
                        bl += weight * (entity.world.getLightLevel(LightType.BLOCK, pos) / MAX_LIGHT_VAL);
                    } else {
                        bl += weight;
                    }
                }
            }
        }

        // The final light value is calculated from the percentage of light contributed out of the total maximum
        int bli = MathHelper.floor((bl / max) * MAX_LIGHTMAP_COORD);
        int sli = MathHelper.floor((sl / max) * MAX_LIGHTMAP_COORD);

        return ((sli & 0xFFFF) << 16) | (bli & 0xFFFF);
    }
}
