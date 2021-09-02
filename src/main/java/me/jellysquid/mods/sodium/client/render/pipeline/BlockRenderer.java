package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.BiomeColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadColorProvider;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkDetailLevel;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.format.MaterialCutoutFlag;
import me.jellysquid.mods.sodium.client.render.chunk.format.MaterialFlag;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.util.rand.XoRoShiRoRandom;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockRenderView;

import java.util.List;
import java.util.Random;

public class BlockRenderer {
    private final Random random = new XoRoShiRoRandom();

    private final BlockColorsExtended blockColors;
    private final BlockOcclusionCache occlusionCache;

    private final QuadLightData cachedQuadLightData = new QuadLightData();

    private final BiomeColorBlender biomeColorBlender;
    private final LightPipelineProvider lighters;

    private final boolean useAmbientOcclusion;

    public BlockRenderer(MinecraftClient client, LightPipelineProvider lighters, BiomeColorBlender biomeColorBlender) {
        this.blockColors = (BlockColorsExtended) client.getBlockColors();
        this.biomeColorBlender = biomeColorBlender;

        this.lighters = lighters;

        this.occlusionCache = new BlockOcclusionCache();
        this.useAmbientOcclusion = MinecraftClient.isAmbientOcclusionEnabled();
    }

    public boolean renderModel(WorldSlice world, BlockState state, BlockPos pos, BlockPos origin, BakedModel model, ChunkBuildBuffers buffers, boolean cull, long seed, int level) {
        RenderLayer layer = RenderLayers.getBlockLayer(state);
        BlockRenderPass pass = this.getRenderPassOverride(state, buffers.getRenderPass(layer));

        if (!this.shouldDrawBlockForDetail(state, level)) {
            return false;
        }

        LightPipeline lighter = this.lighters.getLighter(this.getLightingMode(state, model));
        Vec3d offset = state.getModelOffset(world, pos);

        boolean rendered = false;
        int bits = 0;

        if (layer == RenderLayer.getSolid() || layer == RenderLayer.getTranslucent()) {
            bits |= MaterialCutoutFlag.shift(MaterialCutoutFlag.NONE);
        } else if (layer == RenderLayer.getCutout() || layer == RenderLayer.getTripwire()) {
            bits |= MaterialCutoutFlag.shift(MaterialCutoutFlag.TENTH);
            bits |= MaterialFlag.CUTOUT;
        } else if (layer == RenderLayer.getCutoutMipped()) {
            bits |= MaterialCutoutFlag.shift(MaterialCutoutFlag.HALF);
        }

        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            this.random.setSeed(seed);

            List<BakedQuad> sided = model.getQuads(state, dir, this.random);

            if (sided.isEmpty()) {
                continue;
            }

            if (!cull || this.occlusionCache.shouldDrawSide(state, world, pos, dir)) {
                if (this.shouldDrawSideForDetail(world, state, pos, dir, level)) {
                    this.renderQuadList(world, state, pos, origin, lighter, offset, buffers, pass, sided, ModelQuadFacing.fromDirection(dir), bits);
                    rendered = true;
                }
            }
        }

        this.random.setSeed(seed);

        List<BakedQuad> all = model.getQuads(state, null, this.random);

        if (!all.isEmpty()) {
            this.renderQuadList(world, state, pos, origin, lighter, offset, buffers, pass, all, ModelQuadFacing.UNASSIGNED, bits);

            rendered = true;
        }

        return rendered;
    }

    private BlockRenderPass getRenderPassOverride(BlockState state, BlockRenderPass pass) {
        Block block = state.getBlock();

        if (block instanceof VineBlock || block instanceof PlantBlock || block instanceof LeavesBlock || block instanceof AbstractPlantPartBlock) {
            return BlockRenderPass.OPAQUE_DETAIL;
        }

        return pass;
    }

    private boolean shouldDrawBlockForDetail(BlockState state, int level) {
        Block block = state.getBlock();

        if (level < ChunkDetailLevel.MAXIMUM_DETAIL) {
            return !(block instanceof VineBlock) && !(block instanceof PlantBlock) && !(block instanceof AbstractPlantPartBlock);
        }

        return true;
    }

    private boolean shouldDrawSideForDetail(WorldSlice world, BlockState state, BlockPos pos, Direction dir, int level) {
        if (state.getBlock() instanceof LeavesBlock) {
            BlockState adjState = world.getBlockState(pos.getX() + dir.getOffsetX(), pos.getY() + dir.getOffsetY(), pos.getZ() + dir.getOffsetZ());

            if (adjState.isOpaque() || adjState.getBlock() instanceof LeavesBlock) {
                return level == ChunkDetailLevel.MAXIMUM_DETAIL;
            }
        }

        return true;
    }

    private void renderQuadList(BlockRenderView world, BlockState state, BlockPos pos, BlockPos origin, LightPipeline lighter, Vec3d offset,
                                ChunkBuildBuffers buffers, BlockRenderPass pass, List<BakedQuad> quads, ModelQuadFacing facing, int bits) {
        ChunkModelBuilder builder = buffers.get(pass);
        ModelQuadColorProvider<BlockState> colorizer = null;

        ModelVertexSink vertices = builder.getVertexSink();
        vertices.ensureCapacity(quads.size() * 4);

        IndexBufferBuilder indices = builder.getIndexBufferBuilder(facing);

        // This is a very hot allocation, iterate over it manually
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            BakedQuad quad = quads.get(i);

            QuadLightData light = this.cachedQuadLightData;
            lighter.calculate((ModelQuadView) quad, pos, light, quad.getFace(), quad.hasShade());

            if (quad.hasColor() && colorizer == null) {
                colorizer = this.blockColors.getColorProvider(state);
            }

            this.renderQuad(world, state, pos, origin, vertices, indices, offset, colorizer, quad, light, builder, bits);
        }

        vertices.flush();
    }

    private void renderQuad(BlockRenderView world, BlockState state, BlockPos pos, BlockPos origin, ModelVertexSink vertices, IndexBufferBuilder indices, Vec3d blockOffset,
                            ModelQuadColorProvider<BlockState> colorProvider, BakedQuad bakedQuad, QuadLightData light, ChunkModelBuilder model, int bits) {
        ModelQuadView src = (ModelQuadView) bakedQuad;
        ModelQuadOrientation orientation = ModelQuadOrientation.orientByBrightness(light.br);

        int[] colors = null;

        if (bakedQuad.hasColor()) {
            colors = this.biomeColorBlender.getColors(world, pos, src, colorProvider, state);
        }

        int vertexStart = vertices.getVertexCount();

        for (int i = 0; i < 4; i++) {
            int j = orientation.getVertexIndex(i);

            float x = src.getX(j) + (float) blockOffset.getX();
            float y = src.getY(j) + (float) blockOffset.getY();
            float z = src.getZ(j) + (float) blockOffset.getZ();

            int color = ColorABGR.mul(colors != null ? colors[j] : 0xFFFFFFFF, light.br[j]);

            float u = src.getTexU(j);
            float v = src.getTexV(j);

            int lm = light.lm[j];

            vertices.writeVertex(origin, x, y, z, color, u, v, lm, model.getChunkId(), bits);
        }

        indices.add(vertexStart, ModelQuadWinding.CLOCKWISE);

        Sprite sprite = src.getSprite();

        if (sprite != null) {
            model.addSprite(sprite);
        }
    }

    private LightMode getLightingMode(BlockState state, BakedModel model) {
        if (this.useAmbientOcclusion && model.useAmbientOcclusion() && state.getLuminance() == 0) {
            return LightMode.SMOOTH;
        } else {
            return LightMode.FLAT;
        }
    }
}
