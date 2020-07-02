package me.jellysquid.mods.sodium.client.render.fluid;

import me.jellysquid.mods.sodium.client.model.ModelQuadSinkDelegate;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuad;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.util.ColorARGB;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
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

public class FluidRenderPipeline {
    private final BlockPos.Mutable scratchPos = new BlockPos.Mutable();

    private final Sprite[] lavaSprites = new Sprite[2];
    private final Sprite[] waterSprites = new Sprite[2];
    private final Sprite waterOverlaySprite;

    private final ModelQuadViewMutable quad = new ModelQuad();

    private final LightPipeline smoothLightPipeline;
    private final LightPipeline flatLightPipeline;

    private final QuadLightData quadLightData = new QuadLightData();
    private final BlockPos.Mutable mpos = new BlockPos.Mutable();

    public FluidRenderPipeline(MinecraftClient client, LightPipeline smoothLightPipeline, LightPipeline flatLightPipeline) {
        BlockModels models = client.getBakedModelManager().getBlockModels();

        this.lavaSprites[0] = models.getModel(Blocks.LAVA.getDefaultState()).getSprite();
        this.lavaSprites[1] = ModelLoader.LAVA_FLOW.getSprite();

        this.waterSprites[0] = models.getModel(Blocks.WATER.getDefaultState()).getSprite();
        this.waterSprites[1] = ModelLoader.WATER_FLOW.getSprite();

        this.waterOverlaySprite = ModelLoader.WATER_OVERLAY.getSprite();

        int normal = Norm3b.pack(0.0f, 1.0f, 0.0f);

        for (int i = 0; i < 4; i++) {
            this.quad.setNormal(i, normal);
        }

        this.quad.setFlags(ModelQuadFlags.IS_ALIGNED);

        this.smoothLightPipeline = smoothLightPipeline;
        this.flatLightPipeline = flatLightPipeline;
    }

    private boolean isFluidExposed(WorldSlice world, int x, int y, int z, Fluid fluid) {
        return !world.getFluidState(x, y, z).getFluid().matchesType(fluid);
    }

    private boolean isSideExposed(WorldSlice world, int x, int y, int z, Direction dir, float height) {
        BlockPos pos = this.mpos.set(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ());
        BlockState blockState = world.getBlockState(pos);

        if (blockState.isOpaque()) {
            VoxelShape shape = blockState.getCullingShape(world, pos);

            // Hoist these checks to avoid allocating the shape below
            if (shape == VoxelShapes.fullCube()) {
                // The top face always be inset, so if the shape above is a full cube it can't possibly occlude
                return dir == Direction.UP;
            } else if (shape.isEmpty()) {
                return true;
            }

            VoxelShape threshold = VoxelShapes.cuboid(0.0D, 0.0D, 0.0D, 1.0D, height, 1.0D);

            return !VoxelShapes.isSideCovered(threshold, shape, dir);
        }

        return true;
    }

    public boolean render(ChunkRenderData.Builder meshInfo, WorldSlice world, BlockPos pos, ModelQuadSinkDelegate consumer, FluidState fluidState) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();

        Fluid fluid = fluidState.getFluid();

        boolean sfUp = this.isFluidExposed(world, posX, posY + 1, posZ, fluid);
        boolean sfDown = this.isFluidExposed(world, posX, posY - 1, posZ, fluid) &&
                this.isSideExposed(world, posX, posY, posZ, Direction.DOWN, 0.8888889F);
        boolean sfNorth = this.isFluidExposed(world, posX, posY, posZ - 1, fluid);
        boolean sfSouth = this.isFluidExposed(world, posX, posY, posZ + 1, fluid);
        boolean sfWest = this.isFluidExposed(world, posX - 1, posY, posZ, fluid);
        boolean sfEast = this.isFluidExposed(world, posX + 1, posY, posZ, fluid);

        if (!sfUp && !sfDown && !sfEast && !sfWest && !sfNorth && !sfSouth) {
            return false;
        }

        boolean lava = fluidState.isIn(FluidTags.LAVA);
        Sprite[] sprites = lava ? this.lavaSprites : this.waterSprites;
        int color = lava ? 0xFFFFFF : BiomeColors.getWaterColor(world, pos);

        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        boolean rendered = false;

        float h1 = this.getCornerHeight(world, posX, posY, posZ, fluidState.getFluid());
        float h2 = this.getCornerHeight(world, posX, posY, posZ + 1, fluidState.getFluid());
        float h3 = this.getCornerHeight(world, posX + 1, posY, posZ + 1, fluidState.getFluid());
        float h4 = this.getCornerHeight(world, posX + 1, posY, posZ, fluidState.getFluid());

        float yOffset = sfDown ? 0.001F : 0.0F;

        final ModelQuadViewMutable quad = this.quad;
        final QuadLightData light = this.quadLightData;

        LightPipeline lighter = !lava && MinecraftClient.isAmbientOcclusionEnabled() ? this.smoothLightPipeline : this.flatLightPipeline;

        if (sfUp && this.isSideExposed(world, posX, posY, posZ, Direction.UP, Math.min(Math.min(h1, h2), Math.min(h3, h4)))) {
            h1 -= 0.001F;
            h2 -= 0.001F;
            h3 -= 0.001F;
            h4 -= 0.001F;

            Vec3d velocity = fluidState.getVelocity(world, pos);

            float u1, u2, u3, u4;
            float v1, v2, v3, v4;

            if (velocity.x == 0.0D && velocity.z == 0.0D) {
                Sprite sprite = sprites[0];
                u1 = sprite.getFrameU(0.0D);
                v1 = sprite.getFrameV(0.0D);
                u2 = u1;
                v2 = sprite.getFrameV(16.0D);
                u3 = sprite.getFrameU(16.0D);
                v3 = v2;
                u4 = u3;
                v4 = v1;
            } else {
                Sprite sprite = sprites[1];
                float dir = (float) MathHelper.atan2(velocity.z, velocity.x) - (1.5707964f);
                float sin = MathHelper.sin(dir) * 0.25F;
                float cos = MathHelper.cos(dir) * 0.25F;
                u1 = sprite.getFrameU(8.0F + (-cos - sin) * 16.0F);
                v1 = sprite.getFrameV(8.0F + (-cos + sin) * 16.0F);
                u2 = sprite.getFrameU(8.0F + (-cos + sin) * 16.0F);
                v2 = sprite.getFrameV(8.0F + (cos + sin) * 16.0F);
                u3 = sprite.getFrameU(8.0F + (cos + sin) * 16.0F);
                v3 = sprite.getFrameV(8.0F + (cos - sin) * 16.0F);
                u4 = sprite.getFrameU(8.0F + (cos - sin) * 16.0F);
                v4 = sprite.getFrameV(8.0F + (-cos - sin) * 16.0F);
            }

            float uAvg = (u1 + u2 + u3 + u4) / 4.0F;
            float vAvg = (v1 + v2 + v3 + v4) / 4.0F;
            float s1 = (float) sprites[0].getWidth() / (sprites[0].getMaxU() - sprites[0].getMinU());
            float s2 = (float) sprites[0].getHeight() / (sprites[0].getMaxV() - sprites[0].getMinV());
            float s3 = 4.0F / Math.max(s2, s1);

            u1 = MathHelper.lerp(s3, u1, uAvg);
            u2 = MathHelper.lerp(s3, u2, uAvg);
            u3 = MathHelper.lerp(s3, u3, uAvg);
            u4 = MathHelper.lerp(s3, u4, uAvg);
            v1 = MathHelper.lerp(s3, v1, vAvg);
            v2 = MathHelper.lerp(s3, v2, vAvg);
            v3 = MathHelper.lerp(s3, v3, vAvg);
            v4 = MathHelper.lerp(s3, v4, vAvg);

            this.writeVertex(quad, 0, 0.0f, 0.0f + h1, 0.0f, u1, v1);
            this.writeVertex(quad, 1, 0.0f, 0.0f + h2, 1.0F, u2, v2);
            this.writeVertex(quad, 2, 1.0F, 0.0f + h3, 1.0F, u3, v3);
            this.writeVertex(quad, 3, 1.0F, 0.0f + h4, 0.0f, u4, v4);

            this.applyLighting(quad, pos, lighter, light, Direction.UP);
            this.writeQuad(consumer, quad, red, green, blue, false);

            if (fluidState.method_15756(world, this.scratchPos.set(posX, posY + 1, posZ))) {
                this.writeVertex(quad, 3, 0.0f, 0.0f + h1, 0.0f, u1, v1);
                this.writeVertex(quad, 2, 0.0f, 0.0f + h2, 1.0F, u2, v2);
                this.writeVertex(quad, 1, 1.0F, 0.0f + h3, 1.0F, u3, v3);
                this.writeVertex(quad, 0, 1.0F, 0.0f + h4, 0.0f, u4, v4);
                this.writeQuad(consumer, quad, red, green, blue, true);
            }

            rendered = true;
        }

        if (sfDown) {
            float minU = sprites[0].getMinU();
            float maxU = sprites[0].getMaxU();
            float minV = sprites[0].getMinV();
            float maxV = sprites[0].getMaxV();

            this.writeVertex(quad, 0, 0.0f, 0.0f + yOffset, 1.0F, minU, maxV);
            this.writeVertex(quad, 1, 0.0f, 0.0f + yOffset, 0.0f, minU, minV);
            this.writeVertex(quad, 2, 1.0F, 0.0f + yOffset, 0.0f, maxU, minV);
            this.writeVertex(quad, 3, 1.0F, 0.0f + yOffset, 1.0F, maxU, maxV);

            this.applyLighting(quad, pos, lighter, light, Direction.DOWN);
            this.writeQuad(consumer, quad, red, green, blue, false);

            rendered = true;
        }

        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            float c1;
            float c2;
            float x1;
            float z1;
            float x2;
            float z2;

            switch (dir) {
                case NORTH:
                    if (!sfNorth) {
                        continue;
                    }

                    c1 = h1;
                    c2 = h4;
                    x1 = 0.0f;
                    x2 = 1.0F;
                    z1 = 0.001f;
                    z2 = z1;
                    break;
                case SOUTH:
                    if (!sfSouth) {
                        continue;
                    }

                    c1 = h3;
                    c2 = h2;
                    x1 = 1.0F;
                    x2 = 0.0f;
                    z1 = 0.999f;
                    z2 = z1;
                    break;
                case WEST:
                    if (!sfWest) {
                        continue;
                    }

                    c1 = h2;
                    c2 = h1;
                    x1 = 0.001f;
                    x2 = x1;
                    z1 = 1.0F;
                    z2 = 0.0f;
                    break;
                case EAST:
                    if (!sfEast) {
                        continue;
                    }

                    c1 = h4;
                    c2 = h3;
                    x1 = 0.999f;
                    x2 = x1;
                    z1 = 0.0f;
                    z2 = 1.0F;
                    break;
                default:
                    continue;
            }

            if (this.isSideExposed(world, posX, posY, posZ, dir, Math.max(c1, c2))) {
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

                float u1 = sprite.getFrameU(0.0D);
                float u2 = sprite.getFrameU(8.0D);
                float v1 = sprite.getFrameV((1.0F - c1) * 16.0F * 0.5F);
                float v2 = sprite.getFrameV((1.0F - c2) * 16.0F * 0.5F);
                float v3 = sprite.getFrameV(8.0D);

                float br = dir.getAxis() == Direction.Axis.Z ? 0.8F : 0.6F;

                float redM = br * red;
                float greenM = br * green;
                float blueM = br * blue;

                this.writeVertex(quad, 0, x2, 0.0f + c2, z2, u2, v2);
                this.writeVertex(quad, 1, x2, 0.0f + yOffset, z2, u2, v3);
                this.writeVertex(quad, 2, x1, 0.0f + yOffset, z1, u1, v3);
                this.writeVertex(quad, 3, x1, 0.0f + c1, z1, u1, v1);

                this.applyLighting(quad, pos, lighter, light, dir);
                this.writeQuad(consumer, quad, redM, greenM, blueM, false);

                if (sprite != this.waterOverlaySprite) {
                    this.writeVertex(quad, 0, x1, 0.0f + c1, z1, u1, v1);
                    this.writeVertex(quad, 1, x1, 0.0f + yOffset, z1, u1, v3);
                    this.writeVertex(quad, 2, x2, 0.0f + yOffset, z2, u2, v3);
                    this.writeVertex(quad, 3, x2, 0.0f + c2, z2, u2, v2);

                    this.writeQuad(consumer, quad, redM, greenM, blueM, true);
                }

                rendered = true;
            }
        }

        if (rendered) {
            meshInfo.addSprites(sprites);
        }

        return rendered;
    }

    private void applyLighting(ModelQuadViewMutable quad, BlockPos pos, LightPipeline lighter, QuadLightData light, Direction dir) {
        lighter.calculate(quad, pos, light, dir, true);
    }

    private void writeQuad(ModelQuadSinkDelegate consumer, ModelQuadViewMutable quad, float r, float g, float b, boolean flipLight) {
        QuadLightData quadLightData = this.quadLightData;

        int lightIndex, lightOrder;

        if (flipLight) {
            lightIndex = 3;
            lightOrder = -1;
        } else {
            lightIndex = 0;
            lightOrder = 1;
        }

        for (int i = 0; i < 4; i++) {
            float br = quadLightData.br[lightIndex];
            int lm = quadLightData.lm[lightIndex];

            quad.setColor(i, ColorARGB.pack(r * br, g * br, b * br, 1.0f));
            quad.setLight(i, lm);

            lightIndex += lightOrder;
        }

        consumer.get(ModelQuadFacing.NONE)
                .write(quad);
    }

    private void writeVertex(ModelQuadViewMutable quad, int i, float x, float y, float z, float u, float v) {
        quad.setX(i, x);
        quad.setY(i, y);
        quad.setZ(i, z);
        quad.setTexU(i, u);
        quad.setTexV(i, v);
    }

    private float getCornerHeight(WorldSlice world, int x, int y, int z, Fluid fluid) {
        int samples = 0;
        float totalHeight = 0.0F;

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
                    totalHeight += height * 10.0F;
                    samples += 10;
                } else {
                    totalHeight += height;
                    ++samples;
                }
            } else if (!blockState.getMaterial().isSolid()) {
                ++samples;
            }
        }

        return totalHeight / (float) samples;
    }
}
