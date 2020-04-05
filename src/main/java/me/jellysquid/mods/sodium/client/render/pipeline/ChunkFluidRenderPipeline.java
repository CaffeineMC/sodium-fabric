package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkSlice;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshInfo;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;

public class ChunkFluidRenderPipeline {
    private final BlockPos.Mutable scratchPos = new BlockPos.Mutable();

    private final Sprite[] lavaSprites = new Sprite[2];
    private final Sprite[] waterSprites = new Sprite[2];
    private Sprite waterOverlaySprite;

    public ChunkFluidRenderPipeline() {
        BlockModels models = MinecraftClient.getInstance().getBakedModelManager().getBlockModels();

        this.lavaSprites[0] = models.getModel(Blocks.LAVA.getDefaultState()).getSprite();
        this.lavaSprites[1] = ModelLoader.LAVA_FLOW.getSprite();

        this.waterSprites[0] = models.getModel(Blocks.WATER.getDefaultState()).getSprite();
        this.waterSprites[1] = ModelLoader.WATER_FLOW.getSprite();

        this.waterOverlaySprite = ModelLoader.WATER_OVERLAY.getSprite();
    }

    private static boolean isSameFluid(ChunkSlice world, int x, int y, int z, Fluid fluid) {
        return world.getFluidState(x, y, z).getFluid().matchesType(fluid);
    }

    private static boolean isSideCovered(ChunkSlice world, int x, int y, int z, Direction dir, float height) {
        BlockState blockState = world.getBlockState(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ());

        if (blockState.isOpaque()) {
            VoxelShape a = VoxelShapes.cuboid(0.0D, 0.0D, 0.0D, 1.0D, height, 1.0D);
            VoxelShape b = blockState.getCullingShape(world, new BlockPos(x, y, z));

            return VoxelShapes.isSideCovered(a, b, dir);
        }

        return false;
    }

    public boolean render(ChunkMeshInfo.Builder meshInfo, ChunkSlice world, BlockPos pos, VertexConsumer builder, FluidState fluidState) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();

        Fluid fluid = fluidState.getFluid();

        boolean sfUp = !isSameFluid(world, posX, posY + 1, posZ, fluid);
        boolean sfDown = !isSameFluid(world, posX, posY - 1, posZ, fluid) &&
                !isSideCovered(world, posX, posY, posZ, Direction.DOWN, 0.8888889F);
        boolean sfNorth = !isSameFluid(world, posX, posY, posZ - 1, fluid);
        boolean sfSouth = !isSameFluid(world, posX, posY, posZ + 1, fluid);
        boolean sfWest = !isSameFluid(world, posX - 1, posY, posZ, fluid);
        boolean sfEast = !isSameFluid(world, posX + 1, posY, posZ, fluid);

        if (!sfUp && !sfDown && !sfEast && !sfWest && !sfNorth && !sfSouth) {
            return false;
        }

        boolean lava = fluidState.matches(FluidTags.LAVA);
        Sprite[] sprites = lava ? this.lavaSprites : this.waterSprites;
        int color = lava ? 0xFFFFFF : BiomeColors.getWaterColor(world, pos);

        float baseRed = (float) (color >> 16 & 255) / 255.0F;
        float baseGreen = (float) (color >> 8 & 255) / 255.0F;
        float baseBlue = (float) (color & 255) / 255.0F;

        boolean rendered = false;

        float h1 = this.getNorthWestCornerFluidHeight(world, posX, posY, posZ, fluidState.getFluid());
        float h2 = this.getNorthWestCornerFluidHeight(world, posX, posY, posZ + 1, fluidState.getFluid());
        float h3 = this.getNorthWestCornerFluidHeight(world, posX + 1, posY, posZ + 1, fluidState.getFluid());
        float h4 = this.getNorthWestCornerFluidHeight(world, posX + 1, posY, posZ, fluidState.getFluid());

        double x = pos.getX() & 15;
        double y = pos.getY() & 15;
        double z = pos.getZ() & 15;

        double float_13 = sfDown ? 0.001F : 0.0F;

        if (sfUp && !isSideCovered(world, posX, posY, posZ, Direction.UP, Math.min(Math.min(h1, h2), Math.min(h3, h4)))) {
            rendered = true;

            h1 -= 0.001F;
            h2 -= 0.001F;
            h3 -= 0.001F;
            h4 -= 0.001F;

            Vec3d velocity = fluidState.getVelocity(world, pos);
            float float_14;
            float float_16;
            float float_18;
            float float_20;
            float float_15;
            float float_17;
            float float_19;
            float float_21;

            if (velocity.x == 0.0D && velocity.z == 0.0D) {
                Sprite sprite = sprites[0];
                float_14 = sprite.getFrameU(0.0D);
                float_15 = sprite.getFrameV(0.0D);
                float_16 = float_14;
                float_17 = sprite.getFrameV(16.0D);
                float_18 = sprite.getFrameU(16.0D);
                float_19 = float_17;
                float_20 = float_18;
                float_21 = float_15;
            } else {
                Sprite sprite = sprites[1];
                float float_22 = (float) MathHelper.atan2(velocity.z, velocity.x) - ((float) Math.PI / 2F);
                float float_23 = MathHelper.sin(float_22) * 0.25F;
                float float_24 = MathHelper.cos(float_22) * 0.25F;
                float_14 = sprite.getFrameU(8.0F + (-float_24 - float_23) * 16.0F);
                float_15 = sprite.getFrameV(8.0F + (-float_24 + float_23) * 16.0F);
                float_16 = sprite.getFrameU(8.0F + (-float_24 + float_23) * 16.0F);
                float_17 = sprite.getFrameV(8.0F + (float_24 + float_23) * 16.0F);
                float_18 = sprite.getFrameU(8.0F + (float_24 + float_23) * 16.0F);
                float_19 = sprite.getFrameV(8.0F + (float_24 - float_23) * 16.0F);
                float_20 = sprite.getFrameU(8.0F + (float_24 - float_23) * 16.0F);
                float_21 = sprite.getFrameV(8.0F + (-float_24 - float_23) * 16.0F);
            }

            float float_34 = (float_14 + float_16 + float_18 + float_20) / 4.0F;
            float float_35 = (float_15 + float_17 + float_19 + float_21) / 4.0F;
            float float_36 = (float) sprites[0].getWidth() / (sprites[0].getMaxU() - sprites[0].getMinU());
            float float_37 = (float) sprites[0].getHeight() / (sprites[0].getMaxV() - sprites[0].getMinV());
            float float_38 = 4.0F / Math.max(float_37, float_36);

            float_14 = MathHelper.lerp(float_38, float_14, float_34);
            float_16 = MathHelper.lerp(float_38, float_16, float_34);
            float_18 = MathHelper.lerp(float_38, float_18, float_34);
            float_20 = MathHelper.lerp(float_38, float_20, float_34);
            float_15 = MathHelper.lerp(float_38, float_15, float_35);
            float_17 = MathHelper.lerp(float_38, float_17, float_35);
            float_19 = MathHelper.lerp(float_38, float_19, float_35);
            float_21 = MathHelper.lerp(float_38, float_21, float_35);

            int light = this.getLight(world, posX, posY, posZ);
            float r = 1.0F * baseRed;
            float g = 1.0F * baseGreen;
            float b = 1.0F * baseBlue;
            this.vertex(builder, x + 0.0D, y + (double) h1, z + 0.0D, r, g, b, float_14, float_15, light);
            this.vertex(builder, x + 0.0D, y + (double) h2, z + 1.0D, r, g, b, float_16, float_17, light);
            this.vertex(builder, x + 1.0D, y + (double) h3, z + 1.0D, r, g, b, float_18, float_19, light);
            this.vertex(builder, x + 1.0D, y + (double) h4, z + 0.0D, r, g, b, float_20, float_21, light);

            if (fluidState.method_15756(world, this.scratchPos.set(posX, posY + 1, posZ))) {
                this.vertex(builder, x + 0.0D, y + (double) h1, z + 0.0D, r, g, b, float_14, float_15, light);
                this.vertex(builder, x + 1.0D, y + (double) h4, z + 0.0D, r, g, b, float_20, float_21, light);
                this.vertex(builder, x + 1.0D, y + (double) h3, z + 1.0D, r, g, b, float_18, float_19, light);
                this.vertex(builder, x + 0.0D, y + (double) h2, z + 1.0D, r, g, b, float_16, float_17, light);
            }
        }

        if (sfDown) {
            float minU = sprites[0].getMinU();
            float maxU = sprites[0].getMaxU();
            float minV = sprites[0].getMinV();
            float maxV = sprites[0].getMaxV();
            int light = this.getLight(world, posX, posY - 1, posZ);
            float r = 0.5F * baseRed;
            float g = 0.5F * baseGreen;
            float b = 0.5F * baseBlue;
            this.vertex(builder, x, y + float_13, z + 1.0D, r, g, b, minU, maxV, light);
            this.vertex(builder, x, y + float_13, z, r, g, b, minU, minV, light);
            this.vertex(builder, x + 1.0D, y + float_13, z, r, g, b, maxU, minV, light);
            this.vertex(builder, x + 1.0D, y + float_13, z + 1.0D, r, g, b, maxU, maxV, light);
            rendered = true;
        }

        for (int i = 0; i < 4; ++i) {
            float float_49;
            float float_50;
            double double_4;
            double double_6;
            double double_5;
            double double_7;
            Direction dir;
            boolean boolean_9;
            if (i == 0) {
                float_49 = h1;
                float_50 = h4;
                double_4 = x;
                double_5 = x + 1.0D;
                double_6 = z + (double) 0.001F;
                double_7 = z + (double) 0.001F;
                dir = Direction.NORTH;
                boolean_9 = sfNorth;
            } else if (i == 1) {
                float_49 = h3;
                float_50 = h2;
                double_4 = x + 1.0D;
                double_5 = x;
                double_6 = z + 1.0D - (double) 0.001F;
                double_7 = z + 1.0D - (double) 0.001F;
                dir = Direction.SOUTH;
                boolean_9 = sfSouth;
            } else if (i == 2) {
                float_49 = h2;
                float_50 = h1;
                double_4 = x + (double) 0.001F;
                double_5 = x + (double) 0.001F;
                double_6 = z + 1.0D;
                double_7 = z;
                dir = Direction.WEST;
                boolean_9 = sfWest;
            } else {
                float_49 = h4;
                float_50 = h3;
                double_4 = x + 1.0D - (double) 0.001F;
                double_5 = x + 1.0D - (double) 0.001F;
                double_6 = z;
                double_7 = z + 1.0D;
                dir = Direction.EAST;
                boolean_9 = sfEast;
            }

            if (boolean_9 && !isSideCovered(world, posX, posY, posZ, dir, Math.max(float_49, float_50))) {
                rendered = true;

                int adjX = posX + dir.getOffsetX();
                int adjY = posY + dir.getOffsetY();
                int adjZ = posZ + dir.getOffsetZ();

                Sprite sprite = sprites[1];

                if (!lava) {
                    Block block = world.getBlockState(adjX, adjY, adjZ).getBlock();

                    if (block == Blocks.GLASS || block instanceof StainedGlassBlock) {
                        sprite = this.waterOverlaySprite;
                    }
                }

                float float_57 = sprite.getFrameU(0.0D);
                float float_58 = sprite.getFrameU(8.0D);
                float float_59 = sprite.getFrameV((1.0F - float_49) * 16.0F * 0.5F);
                float float_60 = sprite.getFrameV((1.0F - float_50) * 16.0F * 0.5F);
                float float_61 = sprite.getFrameV(8.0D);

                int light = this.getLight(world, adjX, adjY, adjZ);

                float float_62 = i < 2 ? 0.8F : 0.6F;

                float r = 1.0F * float_62 * baseRed;
                float g = 1.0F * float_62 * baseGreen;
                float b = 1.0F * float_62 * baseBlue;

                this.vertex(builder, double_4, y + (double) float_49, double_6, r, g, b, float_57, float_59, light);
                this.vertex(builder, double_5, y + (double) float_50, double_7, r, g, b, float_58, float_60, light);
                this.vertex(builder, double_5, y + float_13, double_7, r, g, b, float_58, float_61, light);
                this.vertex(builder, double_4, y + float_13, double_6, r, g, b, float_57, float_61, light);

                if (sprite != this.waterOverlaySprite) {
                    this.vertex(builder, double_4, y + float_13, double_6, r, g, b, float_57, float_61, light);
                    this.vertex(builder, double_5, y + float_13, double_7, r, g, b, float_58, float_61, light);
                    this.vertex(builder, double_5, y + (double) float_50, double_7, r, g, b, float_58, float_60, light);
                    this.vertex(builder, double_4, y + (double) float_49, double_6, r, g, b, float_57, float_59, light);
                }
            }
        }

        if (rendered) {
            meshInfo.addSprites(sprites);
        }
        return rendered;
    }

    private void vertex(VertexConsumer consumer, double x, double y, double z, float r, float g, float b, float u, float v, int light) {
        consumer.vertex(x, y, z).color(r, g, b, 1.0F).texture(u, v).light(light).normal(0.0F, 1.0F, 0.0F).next();
    }

    private int getLight(BlockRenderView world, int x, int y, int z) {
        int lm1 = WorldRenderer.getLightmapCoordinates(world, this.scratchPos.set(x, y, z));
        int lm2 = WorldRenderer.getLightmapCoordinates(world, this.scratchPos.set(x, y + 1, z));

        int bl1 = lm1 & 255;
        int bl2 = lm2 & 255;
        int sl1 = lm1 >> 16 & 255;
        int sl2 = lm2 >> 16 & 255;

        return (Math.max(bl1, bl2)) | (Math.max(sl1, sl2)) << 16;
    }

    private float getNorthWestCornerFluidHeight(ChunkSlice world, int x, int y, int z, Fluid fluid) {
        int int_1 = 0;
        float float_1 = 0.0F;

        for (int i = 0; i < 4; ++i) {
            int x2 = x - (i & 1);
            int z2 = z - (i >> 1 & 1);

            if (world.getFluidState(x2, y + 1, z2).getFluid().matchesType(fluid)) {
                return 1.0F;
            }

            BlockState blockState = world.getBlockState(x2, y, z2);
            FluidState fluidState = blockState.getFluidState();

            if (fluidState.getFluid().matchesType(fluid)) {
                float height = fluidState.getHeight(world, this.scratchPos.set(x2, y, z2));

                if (height >= 0.8F) {
                    float_1 += height * 10.0F;
                    int_1 += 10;
                } else {
                    float_1 += height;
                    ++int_1;
                }
            } else if (!blockState.getMaterial().isSolid()) {
                ++int_1;
            }
        }

        return float_1 / (float) int_1;
    }
}
