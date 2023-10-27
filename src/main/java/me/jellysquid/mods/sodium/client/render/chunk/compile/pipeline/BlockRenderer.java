package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import me.jellysquid.mods.sodium.client.frapi.helper.ColorHelper;
import me.jellysquid.mods.sodium.client.frapi.mesh.EncodingFormat;
import me.jellysquid.mods.sodium.client.frapi.mesh.MutableQuadViewImpl;
import me.jellysquid.mods.sodium.client.frapi.render.AbstractBlockRenderContext;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.color.ColorProviderRegistry;
import me.jellysquid.mods.sodium.client.model.light.*;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.util.DirectionUtil;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class BlockRenderer extends AbstractBlockRenderContext {
    private final ColorProviderRegistry colorProviderRegistry;

    private final int[] quadColors = new int[4];
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    // Holders for state used in FRAPI as we can't pass them via parameters
    private ChunkBuildBuffers buffers;
    // Offset of model
    private Vec3d renderOffset;
    // Default AO mode for model (can be overridden by material property)
    private LightMode defaultLightMode;
    // Default material (can be overridden by blend mode per-quad)
    private Material defaultMaterial;
    private ChunkModelBuilder defaultModelBuilder;
    // Colorizer for the current block state
    @Nullable
    private ColorProvider<BlockState> colorizer;

    private final MutableQuadViewImpl editorQuad = new MutableQuadViewImpl() {
        {
            data = new int[EncodingFormat.TOTAL_STRIDE];
            clear();
        }

        @Override
        public void emitDirectly() {
            renderQuad(this);
        }
    };

    public BlockRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lighters) {
        this.colorProviderRegistry = colorRegistry;
        this.lighters = lighters;
    }

    public void renderModel(BlockRenderContext ctx, ChunkBuildBuffers buffers) {
        // Store parameters
        this.ctx = ctx;
        this.buffers = buffers;

        // Clear old state
        this.resetCullState(true);

        // Prepare
        this.renderOffset = ctx.state().getModelOffset(ctx.world(), ctx.pos());
        this.defaultLightMode = this.getLightingMode(ctx.state(), ctx.model());
        this.defaultMaterial = DefaultMaterials.forBlockState(ctx.state());
        this.defaultModelBuilder = buffers.get(this.defaultMaterial);
        this.colorizer = this.colorProviderRegistry.getColorProvider(ctx.state().getBlock());

        // Actually render
        ctx.model().emitBlockQuads(ctx.world(), ctx.state(), ctx.pos(), this.randomSupplier, this);
    }

    /**
     * Process quad, after quad transforms and the culling check have been applied.
     */
    @Override
    protected void processQuad(MutableQuadViewImpl quad) {
        final RenderMaterial mat = quad.material();
        final int colorIndex = mat.disableColorIndex() ? -1 : quad.colorIndex();
        final TriState aoMode = mat.ambientOcclusion();
        final LightMode lightMode;
        if (aoMode == TriState.DEFAULT) {
            lightMode = this.defaultLightMode;
        } else {
            lightMode = this.useAmbientOcclusion && aoMode.get() ? LightMode.SMOOTH : LightMode.FLAT;
        }
        final boolean emissive = mat.emissive();
        final BlendMode blendMode = mat.blendMode();
        final Material material;
        final ChunkModelBuilder modelBuilder;
        if (blendMode == BlendMode.DEFAULT) {
            material = this.defaultMaterial;
            modelBuilder = this.defaultModelBuilder;
        } else {
            material = DefaultMaterials.forRenderLayer(blendMode.blockRenderLayer);
            modelBuilder = this.buffers.get(material);
        }

        BlockRenderContext ctx = this.ctx;

        this.colorizeQuad(ctx, quad, colorIndex);
        QuadLightData lightData = this.quadLightData;
        this.shadeQuad(ctx, quad, lightMode, emissive, lightData);
        this.bufferQuad(ctx, quad, lightData.br, material, modelBuilder);
    }

    private void colorizeQuad(BlockRenderContext ctx, MutableQuadViewImpl quad, int colorIndex) {
        if (colorIndex != -1) {
            ColorProvider<BlockState> colorProvider = this.colorizer;
            int[] vertexColors = this.quadColors;

            if (colorProvider != null) {
                // I'm sorry for this cast Jelly
                // TODO: better solution
                colorProvider.getColors((WorldSlice) ctx.world(), ctx.pos(), ctx.state(), quad, vertexColors);

                for (int i = 0; i < 4; i++) {
                    // Set alpha to 0xFF in case a quad transform inspects the color.
                    // We do not support per-vertex alpha, however, so this will get discarded at vertex encoding time.
                    quad.color(i, ColorHelper.multiplyColor(0xFF000000 | vertexColors[i], quad.color(i)));
                }
            }
        }
    }

    private void bufferQuad(BlockRenderContext ctx, MutableQuadViewImpl quad, float[] brightness, Material material, ChunkModelBuilder modelBuilder) {
        ModelQuadOrientation orientation = ModelQuadOrientation.orientByBrightness(brightness, quad);
        ChunkVertexEncoder.Vertex[] vertices = this.vertices;

        // TODO: this should be precomputed and stored in the QuadViewImpl
        ModelQuadFacing normalFace = ModelQuadUtil.findNormalFace(quad.packedFaceNormal());
        Vec3d offset = this.renderOffset;

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = orientation.getVertexIndex(dstIndex);

            var out = vertices[dstIndex];
            out.x = ctx.origin().x() + quad.x(srcIndex) + (float) offset.getX();
            out.y = ctx.origin().y() + quad.y(srcIndex) + (float) offset.getY();
            out.z = ctx.origin().z() + quad.z(srcIndex) + (float) offset.getZ();

            // FRAPI implementation uses ARGB color format, convert to ABGR.
            // Due to our vertex format, the alpha from the quad color is ignored entirely.
            out.color = ColorABGR.withAlpha(ColorARGB.toABGR(quad.color(srcIndex)), brightness[srcIndex]);

            out.u = quad.u(srcIndex);
            out.v = quad.v(srcIndex);

            out.light = quad.lightmap(srcIndex);
        }

        var vertexBuffer = modelBuilder.getVertexBuffer(normalFace);
        vertexBuffer.push(vertices, material);

        modelBuilder.addSprite(quad.getSprite(this.spriteFinder));
    }

    @Override
    public QuadEmitter getEmitter() {
        editorQuad.clear();
        return editorQuad;
    }
}
