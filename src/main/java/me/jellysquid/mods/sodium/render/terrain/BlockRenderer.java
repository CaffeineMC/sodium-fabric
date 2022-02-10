package me.jellysquid.mods.sodium.render.terrain;

import me.jellysquid.mods.sodium.render.terrain.light.LightMode;
import me.jellysquid.mods.sodium.render.terrain.light.LightPipeline;
import me.jellysquid.mods.sodium.render.terrain.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.render.terrain.light.data.QuadLightData;
import me.jellysquid.mods.sodium.render.terrain.quad.ModelQuadView;
import me.jellysquid.mods.sodium.render.terrain.color.blender.ColorBlender;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.render.terrain.color.ColorSampler;
import me.jellysquid.mods.sodium.render.chunk.compile.buffers.ChunkMeshBuilder;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexSink;
import me.jellysquid.mods.sodium.util.packed.ColorABGR;
import me.jellysquid.mods.sodium.util.rand.XoRoShiRoRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import me.jellysquid.mods.sodium.interop.vanilla.mixin.BlockColorProviderRegistry;
import me.jellysquid.mods.sodium.util.DirectionUtil;
import java.util.List;
import java.util.Random;

public class BlockRenderer {
    private final Random random = new XoRoShiRoRandom();

    private final BlockColorProviderRegistry blockColors;
    private final BlockOcclusionCache occlusionCache;

    private final QuadLightData cachedQuadLightData = new QuadLightData();

    private final ColorBlender colorBlender;
    private final LightPipelineProvider lighters;

    private final boolean useAmbientOcclusion;

    public BlockRenderer(Minecraft client, LightPipelineProvider lighters, ColorBlender colorBlender) {
        this.blockColors = (BlockColorProviderRegistry) client.getBlockColors();
        this.colorBlender = colorBlender;

        this.lighters = lighters;

        this.occlusionCache = new BlockOcclusionCache();
        this.useAmbientOcclusion = Minecraft.useAmbientOcclusion();
    }

    public boolean renderModel(BlockAndTintGetter world, BlockState state, BlockPos pos, BlockPos origin, BakedModel model, ChunkMeshBuilder buffers, boolean cull, long seed) {
        LightPipeline lighter = this.lighters.getLighter(this.getLightingMode(state, model));
        Vec3 offset = state.getOffset(world, pos);

        boolean rendered = false;

        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            this.random.setSeed(seed);

            List<BakedQuad> sided = model.getQuads(state, dir, this.random);

            if (sided.isEmpty()) {
                continue;
            }

            if (!cull || this.occlusionCache.shouldDrawSide(state, world, pos, dir)) {
                this.renderQuadList(world, state, pos, origin, lighter, offset, buffers, sided, ChunkMeshFace.fromDirection(dir));

                rendered = true;
            }
        }

        this.random.setSeed(seed);

        List<BakedQuad> all = model.getQuads(state, null, this.random);

        if (!all.isEmpty()) {
            this.renderQuadList(world, state, pos, origin, lighter, offset, buffers, all, ChunkMeshFace.UNASSIGNED);

            rendered = true;
        }

        return rendered;
    }

    private void renderQuadList(BlockAndTintGetter world, BlockState state, BlockPos pos, BlockPos origin, LightPipeline lighter, Vec3 offset,
                                ChunkMeshBuilder buffers, List<BakedQuad> quads, ChunkMeshFace facing) {
        ColorSampler<BlockState> colorizer = null;

        TerrainVertexSink vertices = buffers.getVertexSink(facing);
        vertices.ensureCapacity(quads.size() * 4);

        // This is a very hot allocation, iterate over it manually
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            BakedQuad quad = quads.get(i);

            QuadLightData light = this.cachedQuadLightData;
            lighter.calculate((ModelQuadView) quad, pos, light, quad.getDirection(), quad.isShade());

            if (quad.isTinted() && colorizer == null) {
                colorizer = this.blockColors.getColorProvider(state);
            }

            this.renderQuad(world, state, pos, origin, vertices, offset, colorizer, quad, light, buffers);
        }

        vertices.flush();
    }

    private void renderQuad(BlockAndTintGetter world, BlockState state, BlockPos pos, BlockPos origin, TerrainVertexSink vertices, Vec3 blockOffset,
                            ColorSampler<BlockState> colorSampler, BakedQuad bakedQuad, QuadLightData light, ChunkMeshBuilder model) {
        ModelQuadView src = (ModelQuadView) bakedQuad;
        ModelQuadOrientation orientation = ModelQuadOrientation.orientByBrightness(light.br);

        int[] colors = null;

        if (bakedQuad.isTinted()) {
            colors = this.colorBlender.getColors(world, pos, src, colorSampler, state);
        }
        
        for (int i = 0; i < 4; i++) {
            int j = orientation.getVertexIndex(i);

            float x = src.getX(j) + (float) blockOffset.x();
            float y = src.getY(j) + (float) blockOffset.y();
            float z = src.getZ(j) + (float) blockOffset.z();

            int color = ColorABGR.mul(colors != null ? colors[j] : 0xFFFFFFFF, light.br[j]);

            float u = src.getTexU(j);
            float v = src.getTexV(j);

            int lm = light.lm[j];

            vertices.writeVertex(origin, x, y, z, color, u, v, lm);
        }

        TextureAtlasSprite sprite = src.getSprite();

        if (sprite != null) {
            model.addSprite(sprite);
        }
    }

    private LightMode getLightingMode(BlockState state, BakedModel model) {
        if (this.useAmbientOcclusion && model.useAmbientOcclusion() && state.getLightEmission() == 0) {
            return LightMode.SMOOTH;
        } else {
            return LightMode.FLAT;
        }
    }
}
