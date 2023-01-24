package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorSampler;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;

import java.util.List;

public class BlockRenderer {
    private final Random random = new LocalRandom(42L);

    private final BlockColorsExtended blockColors;
    private final BlockOcclusionCache occlusionCache;

    private final QuadLightData cachedQuadLightData = new QuadLightData();

    private final ColorBlender colorBlender;
    private final LightPipelineProvider lighters;

    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private final boolean useAmbientOcclusion;

    public BlockRenderer(MinecraftClient client, LightPipelineProvider lighters, ColorBlender colorBlender) {
        this.blockColors = (BlockColorsExtended) client.getBlockColors();
        this.colorBlender = colorBlender;

        this.lighters = lighters;

        this.occlusionCache = new BlockOcclusionCache();
        this.useAmbientOcclusion = MinecraftClient.isAmbientOcclusionEnabled();
    }

    public boolean renderModel(BlockRenderContext ctx, ChunkModelBuilder buffers) {
        LightPipeline lighter = this.lighters.getLighter(this.getLightingMode(ctx.state(), ctx.model()));
        Vec3d renderOffset = ctx.state().getModelOffset(ctx.world(), ctx.pos());

        var rendered = false;

        for (Direction face : DirectionUtil.ALL_DIRECTIONS) {
            List<BakedQuad> quads = this.getGeometry(ctx, face);

            if (!quads.isEmpty() && this.isFaceVisible(ctx, face)) {
                this.renderQuadList(ctx, lighter, renderOffset, buffers, quads, face);
                rendered = true;
            }
        }

        List<BakedQuad> all = this.getGeometry(ctx, null);

        if (!all.isEmpty()) {
            this.renderQuadList(ctx, lighter, renderOffset, buffers, all, null);
            rendered = true;
        }

        return rendered;
    }

    private List<BakedQuad> getGeometry(BlockRenderContext ctx, Direction face) {
        var random = this.random;
        random.setSeed(ctx.seed());

        return ctx.model().getQuads(ctx.state(), face, random);
    }

    private boolean isFaceVisible(BlockRenderContext ctx, Direction face) {
        return this.occlusionCache.shouldDrawSide(ctx.state(), ctx.world(), ctx.pos(), face);
    }

    private void renderQuadList(BlockRenderContext ctx, LightPipeline lighter, Vec3d offset,
                                ChunkModelBuilder builder, List<BakedQuad> quads, Direction cullFace) {
        ModelQuadFacing facing = cullFace == null ? ModelQuadFacing.UNASSIGNED : ModelQuadFacing.fromDirection(cullFace);
        ColorSampler<BlockState> colorizer = null;

        ChunkVertexBufferBuilder vertexBuffer = builder.getVertexBuffer();
        IndexBufferBuilder indexBuffer = builder.getIndexBuffer(facing);

        QuadLightData lightData = this.cachedQuadLightData;

        // This is a very hot allocation, iterate over it manually
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            BakedQuad quad = quads.get(i);
            ModelQuadView quadView = (ModelQuadView) quad;

            lighter.calculate(quadView, ctx.pos(), lightData, cullFace, quad.getFace(), quad.hasShade());

            int[] colors = null;

            if (quad.hasColor()) {
                if (colorizer == null) {
                    colorizer = this.blockColors.getColorProvider(ctx.state());
                }

                colors = this.colorBlender.getColors(ctx.world(), ctx.pos(), quadView, colorizer, ctx.state());
            }

            this.writeGeometry(ctx, vertexBuffer, indexBuffer, offset, quadView, colors, lightData.br, lightData.lm);

            Sprite sprite = quad.getSprite();

            if (sprite != null) {
                builder.addSprite(sprite);
            }
        }
    }

    private void writeGeometry(BlockRenderContext ctx,
                               ChunkVertexBufferBuilder vertexBuffer, IndexBufferBuilder indexBuffer,
                               Vec3d offset,
                               ModelQuadView quad,
                               int[] colors,
                               float[] brightness,
                               int[] lightmap)
    {
        ModelQuadOrientation orientation = ModelQuadOrientation.orientByBrightness(brightness);
        var vertices = this.vertices;

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = orientation.getVertexIndex(dstIndex);

            var out = vertices[dstIndex];
            out.x = ctx.origin().x() + quad.getX(srcIndex) + (float) offset.getX();
            out.y = ctx.origin().y() + quad.getY(srcIndex) + (float) offset.getY();
            out.z = ctx.origin().z() + quad.getZ(srcIndex) + (float) offset.getZ();

            out.color = ColorABGR.mul(colors != null ? colors[srcIndex] : 0xFFFFFFFF, brightness[srcIndex]);

            out.u = quad.getTexU(srcIndex);
            out.v = quad.getTexV(srcIndex);

            out.light = lightmap[srcIndex];
        }

        indexBuffer.add(vertexBuffer.push(vertices), ModelQuadWinding.CLOCKWISE);
    }

    private LightMode getLightingMode(BlockState state, BakedModel model) {
        if (this.useAmbientOcclusion && model.useAmbientOcclusion() && state.getLuminance() == 0) {
            return LightMode.SMOOTH;
        } else {
            return LightMode.FLAT;
        }
    }
}
