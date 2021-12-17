package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuad;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadColorProvider;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.BiomeColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshBuilderDelegate;
import me.jellysquid.mods.sodium.client.render.chunk.passes.ChunkMeshType;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import me.jellysquid.mods.sodium.client.util.rand.XoRoShiRoRandom;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
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

    public boolean renderBlock(BlockRenderView world, BlockState state, BlockPos pos, BlockPos origin, BakedModel model, ChunkBuildBuffers buffers, RenderLayer layer, boolean cull, long seed) {
        if (this.isSimpleCubeModel(state, model, seed)) {
            return this.renderCube(world, state, pos, origin, model, buffers.get(layer, ChunkMeshType.CUBE), cull, seed);
        } else {
            return this.renderModel(world, state, pos, origin, model, buffers.get(layer, ChunkMeshType.MODEL), cull, seed);
        }
    }

    @Deprecated(forRemoval = true) // This is a hack
    private boolean isSimpleCubeModel(BlockState state, BakedModel model, long seed) {
        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            this.random.setSeed(seed);

            for (BakedQuad quad : model.getQuads(state, dir, this.random)) {
                if (((ModelQuadView) quad).getFlags() != ModelQuadFlags.IS_ALIGNED) {
                    return false;
                }
            }
        }

        this.random.setSeed(seed);

        List<BakedQuad> all = model.getQuads(state, null, this.random);

        return all.isEmpty();
    }

    private boolean renderCube(BlockRenderView world, BlockState state, BlockPos pos, BlockPos origin, BakedModel model, ChunkMeshBuilderDelegate<?> delegate, boolean cull, long seed) {
        LightPipeline lighter = this.lighters.getLighter(this.getLightingMode(state, model));

        boolean rendered = false;

        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            this.random.setSeed(seed);

            List<BakedQuad> sided = model.getQuads(state, dir, this.random);

            if (sided.isEmpty()) {
                continue;
            }

            if (!cull || this.occlusionCache.shouldDrawSide(state, world, pos, dir)) {
                for (BakedQuad quad : sided) {
                    this.renderCubeFace(world, state, pos, origin, lighter, delegate, dir, quad);
                }

                rendered = true;
            }
        }

        return rendered;
    }

    private void renderCubeFace(BlockRenderView world, BlockState state, BlockPos pos, BlockPos origin, LightPipeline lighter, ChunkMeshBuilderDelegate<?> delegate, Direction direction, BakedQuad bakedQuad) {
        ModelQuadView quad = (ModelQuadView) bakedQuad;

        QuadLightData light = this.cachedQuadLightData;
        lighter.calculate(quad, pos, light, bakedQuad.getFace(), bakedQuad.hasShade());

        int[] colors = null;

        if (bakedQuad.hasColor()) {
            colors = this.biomeColorBlender.getColors(world, pos, quad, this.blockColors.getColorProvider(state), state);
        }

        ModelQuad copy = new ModelQuad();

        for (int i = 0; i < 4; i++) {
            copy.setColor(i, ColorARGB.toABGR(colors != null ? colors[i] : 0xFFFFFFFF, light.br[i]));

            copy.setTexU(i, quad.getTexU(i));
            copy.setTexV(i, quad.getTexV(i));

            copy.setLight(i, light.lm[i]);
        }

        delegate.addQuad(origin, copy, ModelQuadFacing.fromDirection(direction));
    }

    private boolean renderModel(BlockRenderView world, BlockState state, BlockPos pos, BlockPos origin, BakedModel model, ChunkMeshBuilderDelegate<?> delegate, boolean cull, long seed) {
        LightPipeline lighter = this.lighters.getLighter(this.getLightingMode(state, model));
        Vec3d offset = state.getModelOffset(world, pos);

        boolean rendered = false;

        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            this.random.setSeed(seed);

            List<BakedQuad> sided = model.getQuads(state, dir, this.random);

            if (sided.isEmpty()) {
                continue;
            }

            if (!cull || this.occlusionCache.shouldDrawSide(state, world, pos, dir)) {
                this.renderQuadList(world, state, pos, origin, lighter, offset, delegate, sided, ModelQuadFacing.fromDirection(dir));

                rendered = true;
            }
        }

        this.random.setSeed(seed);

        List<BakedQuad> all = model.getQuads(state, null, this.random);

        if (!all.isEmpty()) {
            this.renderQuadList(world, state, pos, origin, lighter, offset, delegate, all, ModelQuadFacing.UNASSIGNED);

            rendered = true;
        }

        return rendered;
    }

    private void renderQuadList(BlockRenderView world, BlockState state, BlockPos pos, BlockPos origin, LightPipeline lighter, Vec3d offset,
                                ChunkMeshBuilderDelegate<?> delegate, List<BakedQuad> quads, ModelQuadFacing facing) {
        ModelQuadColorProvider<BlockState> colorizer = null;

        // This is a very hot allocation, iterate over it manually
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            BakedQuad modelQuad = quads.get(i);

            QuadLightData light = this.cachedQuadLightData;
            lighter.calculate((ModelQuadView) modelQuad, pos, light, modelQuad.getFace(), modelQuad.hasShade());

            if (modelQuad.hasColor() && colorizer == null) {
                colorizer = this.blockColors.getColorProvider(state);
            }

            ModelQuad quad = this.buildQuad(world, state, pos, offset, colorizer, modelQuad, light);
            delegate.addQuad(origin, quad, facing);
        }
    }

    private ModelQuad buildQuad(BlockRenderView world, BlockState state, BlockPos pos, Vec3d modelOffset,
                                ModelQuadColorProvider<BlockState> colorProvider, BakedQuad bakedQuad, QuadLightData light) {
        ModelQuadView src = (ModelQuadView) bakedQuad;
        ModelQuadOrientation orientation = ModelQuadOrientation.orientByBrightness(light.br);

        int[] colors = null;

        if (bakedQuad.hasColor()) {
            colors = this.biomeColorBlender.getColors(world, pos, src, colorProvider, state);
        }

        ModelQuad dst = new ModelQuad();

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = orientation.getVertexIndex(dstIndex);

            dst.setX(dstIndex, src.getX(srcIndex) + (float) modelOffset.getX());
            dst.setY(dstIndex, src.getY(srcIndex) + (float) modelOffset.getY());
            dst.setZ(dstIndex, src.getZ(srcIndex) + (float) modelOffset.getZ());

            dst.setColor(dstIndex, ColorARGB.toABGR(colors != null ? colors[srcIndex] : 0xFFFFFFFF, light.br[srcIndex]));

            dst.setTexU(dstIndex, src.getTexU(srcIndex));
            dst.setTexV(dstIndex, src.getTexV(srcIndex));

            dst.setLight(dstIndex, light.lm[srcIndex]);
        }

        return dst;
    }

    private LightMode getLightingMode(BlockState state, BakedModel model) {
        if (this.useAmbientOcclusion && model.useAmbientOcclusion() && state.getLuminance() == 0) {
            return LightMode.SMOOTH;
        } else {
            return LightMode.FLAT;
        }
    }
}
