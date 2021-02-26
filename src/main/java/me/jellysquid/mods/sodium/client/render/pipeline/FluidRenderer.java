package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuad;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.model.quad.blender.BiomeColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.coderbot.iris.Iris;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColorProvider;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockRenderView;

public class FluidRenderer {
    private static final BlockColorProvider FLUID_COLOR_PROVIDER = (state, world, pos, tintIndex) -> {
        if (world == null) return 0xFFFFFFFF;
        return BiomeColors.getWaterColor(world, pos);
    };

    private final BlockPos.Mutable scratchPos = new BlockPos.Mutable();

    private final Sprite[] lavaSprites = new Sprite[2];
    private final Sprite[] waterSprites = new Sprite[2];
    private final Sprite waterOverlaySprite;

    private final ModelQuadViewMutable quad = new ModelQuad();

    private final LightPipelineProvider lighters;
    private final BiomeColorBlender biomeColorBlender;

    private final QuadLightData quadLightData = new QuadLightData();
    private final int[] quadColors = new int[4];

    public FluidRenderer(MinecraftClient client, LightPipelineProvider lighters, BiomeColorBlender biomeColorBlender) {
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

        this.lighters = lighters;
        this.biomeColorBlender = biomeColorBlender;
    }

    private boolean isFluidExposed(BlockRenderView world, int x, int y, int z, Fluid fluid) {
        BlockPos pos = this.scratchPos.set(x, y, z);
        return !world.getFluidState(pos).getFluid().matchesType(fluid);
    }

    private boolean isSideExposed(BlockRenderView world, int x, int y, int z, Direction dir, float height) {
        BlockPos pos = this.scratchPos.set(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ());
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

    public boolean render(BlockRenderView world, FluidState fluidState, BlockPos pos, ChunkModelBuffers buffers) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();

        Fluid fluid = fluidState.getFluid();
        BlockState state = fluidState.getBlockState();
        Identifier id = Registry.BLOCK.getId(state.getBlock());
        short blockId = (short) (int) Iris.getCurrentPack().getIdMap().getBlockProperties().getOrDefault(id, -1);

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

        boolean rendered = false;

        float h1 = this.getCornerHeight(world, posX, posY, posZ, fluidState.getFluid());
        float h2 = this.getCornerHeight(world, posX, posY, posZ + 1, fluidState.getFluid());
        float h3 = this.getCornerHeight(world, posX + 1, posY, posZ + 1, fluidState.getFluid());
        float h4 = this.getCornerHeight(world, posX + 1, posY, posZ, fluidState.getFluid());

        float yOffset = sfDown ? 0.001F : 0.0F;

        final ModelQuadViewMutable quad = this.quad;

        LightMode lightMode = !lava && MinecraftClient.isAmbientOcclusionEnabled() ? LightMode.SMOOTH : LightMode.FLAT;
        LightPipeline lighter = this.lighters.getLighter(lightMode);

        quad.setFlags(0);

        if (sfUp && this.isSideExposed(world, posX, posY, posZ, Direction.UP, Math.min(Math.min(h1, h2), Math.min(h3, h4)))) {
            h1 -= 0.001F;
            h2 -= 0.001F;
            h3 -= 0.001F;
            h4 -= 0.001F;

            Vec3d velocity = fluidState.getVelocity(world, pos);

            Sprite sprite;
            ModelQuadFacing facing;
            float u1, u2, u3, u4;
            float v1, v2, v3, v4;

            if (velocity.x == 0.0D && velocity.z == 0.0D) {
                sprite = sprites[0];
                facing = ModelQuadFacing.UP;
                u1 = sprite.getFrameU(0.0D);
                v1 = sprite.getFrameV(0.0D);
                u2 = u1;
                v2 = sprite.getFrameV(16.0D);
                u3 = sprite.getFrameU(16.0D);
                v3 = v2;
                u4 = u3;
                v4 = v1;
            } else {
                sprite = sprites[1];
                facing = ModelQuadFacing.UNASSIGNED;
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

            quad.setSprite(sprite);

            this.setVertex(quad, 0, 0.0f, h1, 0.0f, u1, v1);
            this.setVertex(quad, 1, 0.0f, h2, 1.0F, u2, v2);
            this.setVertex(quad, 2, 1.0F, h3, 1.0F, u3, v3);
            this.setVertex(quad, 3, 1.0F, h4, 0.0f, u4, v4);

            this.calculateQuadColors(quad, world, pos, lighter, Direction.UP, 1.0F, !lava);
            this.flushQuad(buffers, quad, facing, false, blockId);

            if (fluidState.method_15756(world, this.scratchPos.set(posX, posY + 1, posZ))) {
                this.setVertex(quad, 3, 0.0f, h1, 0.0f, u1, v1);
                this.setVertex(quad, 2, 0.0f, h2, 1.0F, u2, v2);
                this.setVertex(quad, 1, 1.0F, h3, 1.0F, u3, v3);
                this.setVertex(quad, 0, 1.0F, h4, 0.0f, u4, v4);

                this.flushQuad(buffers, quad, ModelQuadFacing.DOWN, true, blockId);
            }

            rendered = true;
        }

        if (sfDown) {
            Sprite sprite = sprites[0];

            float minU = sprite.getMinU();
            float maxU = sprite.getMaxU();
            float minV = sprite.getMinV();
            float maxV = sprite.getMaxV();
            quad.setSprite(sprite);

            this.setVertex(quad, 0, 0.0f, yOffset, 1.0F, minU, maxV);
            this.setVertex(quad, 1, 0.0f, yOffset, 0.0f, minU, minV);
            this.setVertex(quad, 2, 1.0F, yOffset, 0.0f, maxU, minV);
            this.setVertex(quad, 3, 1.0F, yOffset, 1.0F, maxU, maxV);

            this.calculateQuadColors(quad, world, pos, lighter, Direction.DOWN, 1.0F, !lava);
            this.flushQuad(buffers, quad, ModelQuadFacing.DOWN, false, blockId);

            rendered = true;
        }

        this.quad.setFlags(ModelQuadFlags.IS_ALIGNED);

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
                    BlockPos posAdj = this.scratchPos.set(adjX, adjY, adjZ);
                    Block block = world.getBlockState(posAdj).getBlock();

                    if (block == Blocks.GLASS || block instanceof StainedGlassBlock) {
                        sprite = this.waterOverlaySprite;
                    }
                }

                float u1 = sprite.getFrameU(0.0D);
                float u2 = sprite.getFrameU(8.0D);
                float v1 = sprite.getFrameV((1.0F - c1) * 16.0F * 0.5F);
                float v2 = sprite.getFrameV((1.0F - c2) * 16.0F * 0.5F);
                float v3 = sprite.getFrameV(8.0D);

                quad.setSprite(sprite);

                this.setVertex(quad, 0, x2, c2, z2, u2, v2);
                this.setVertex(quad, 1, x2, yOffset, z2, u2, v3);
                this.setVertex(quad, 2, x1, yOffset, z1, u1, v3);
                this.setVertex(quad, 3, x1, c1, z1, u1, v1);

                float br = dir.getAxis() == Direction.Axis.Z ? 0.8F : 0.6F;

                this.calculateQuadColors(quad, world, pos, lighter, dir, br, !lava);
                this.flushQuad(buffers, quad, ModelQuadFacing.fromDirection(dir), false, blockId);

                if (sprite != this.waterOverlaySprite) {
                    this.setVertex(quad, 0, x1, c1, z1, u1, v1);
                    this.setVertex(quad, 1, x1, yOffset, z1, u1, v3);
                    this.setVertex(quad, 2, x2, yOffset, z2, u2, v3);
                    this.setVertex(quad, 3, x2, c2, z2, u2, v2);

                    this.flushQuad(buffers, quad, ModelQuadFacing.fromDirection(dir), true, blockId);
                }

                rendered = true;
            }
        }

        return rendered;
    }

    private void calculateQuadColors(ModelQuadView quad, BlockRenderView world,  BlockPos pos, LightPipeline lighter, Direction dir, float brightness, boolean colorized) {
        QuadLightData light = this.quadLightData;
        lighter.calculate(quad, pos, light, dir, false);

        int[] biomeColors = null;

        if (colorized) {
            biomeColors = this.biomeColorBlender.getColors(FLUID_COLOR_PROVIDER, world, Blocks.WATER.getDefaultState(), pos, quad);
        }

        for (int i = 0; i < 4; i++) {
            this.quadColors[i] = ColorABGR.mul(biomeColors != null ? biomeColors[i] : 0xFFFFFFFF, light.br[i] * brightness);
        }
    }

    private void flushQuad(ChunkModelBuffers buffers, ModelQuadView quad, ModelQuadFacing facing, boolean flip, short blockId) {
        int vertexIdx, lightOrder;

        if (flip) {
            vertexIdx = 3;
            lightOrder = -1;
        } else {
            vertexIdx = 0;
            lightOrder = 1;
        }

        ModelVertexSink sink = buffers.getSink(facing);
        sink.ensureCapacity(4);

        for (int i = 0; i < 4; i++) {
            float x = quad.getX(i);
            float y = quad.getY(i);
            float z = quad.getZ(i);

            int color = this.quadColors[vertexIdx];

            float u = quad.getTexU(i);
            float v = quad.getTexV(i);

            int light = this.quadLightData.lm[vertexIdx];

            sink.writeQuad(x, y, z, color, u, v, light, blockId);

            vertexIdx += lightOrder;
        }

        Sprite sprite = quad.getSprite();

        if (sprite != null) {
            buffers.getRenderData().addSprite(sprite);
        }

        sink.flush();
    }

    private void setVertex(ModelQuadViewMutable quad, int i, float x, float y, float z, float u, float v) {
        quad.setX(i, x);
        quad.setY(i, y);
        quad.setZ(i, z);
        quad.setTexU(i, u);
        quad.setTexV(i, v);
    }

    private float getCornerHeight(BlockRenderView world, int x, int y, int z, Fluid fluid) {
        int samples = 0;
        float totalHeight = 0.0F;

        for (int i = 0; i < 4; ++i) {
            int x2 = x - (i & 1);
            int z2 = z - (i >> 1 & 1);

            if (world.getFluidState(this.scratchPos.set(x2, y + 1, z2)).getFluid().matchesType(fluid)) {
                return 1.0F;
            }

            BlockPos pos = this.scratchPos.set(x2, y, z2);

            BlockState blockState = world.getBlockState(pos);
            FluidState fluidState = blockState.getFluidState();

            if (fluidState.getFluid().matchesType(fluid)) {
                float height = fluidState.getHeight(world, pos);

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
