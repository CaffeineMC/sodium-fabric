package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.color.ColorProviderRegistry;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.render.frapi.SpriteFinderCache;
import me.jellysquid.mods.sodium.client.render.frapi.helper.ColorHelper;
import me.jellysquid.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import me.jellysquid.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.LocalRandom;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class BlockRenderer extends AbstractBlockRenderContext {
    private final ColorProviderRegistry colorProviderRegistry;
    private final int[] vertexColors = new int[4];
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private ChunkBuildBuffers buffers;
    private WorldSlice slice;

    private final Vector3f posOffset = new Vector3f();
    @Nullable
    private ColorProvider<BlockState> colorProvider;
    private Material defaultMaterial;
    private ChunkModelBuilder defaultModelBuilder;

    public BlockRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lighters) {
        this.colorProviderRegistry = colorRegistry;
        this.lighters = lighters;

        this.random = new LocalRandom(42L);
    }

    public void prepare(ChunkBuildBuffers buffers, WorldSlice world) {
        this.buffers = buffers;
        this.world = world;
        this.slice = world;
    }

    public void release() {
        this.buffers = null;
        this.world = null;
        this.slice = null;
    }

    public void renderModel(BakedModel model, BlockState state, BlockPos pos, BlockPos origin) {
        this.state = state;
        this.pos = pos;

        this.randomSeed = state.getRenderingSeed(pos);

        this.posOffset.set(origin.getX(), origin.getY(), origin.getZ());
        if (state.hasModelOffset()) {
            Vec3d modelOffset = state.getModelOffset(this.world, pos);
            this.posOffset.add((float) modelOffset.x, (float) modelOffset.y, (float) modelOffset.z);
        }

        this.colorProvider = this.colorProviderRegistry.getColorProvider(state.getBlock());
        this.defaultMaterial = DefaultMaterials.forBlockState(state);
        this.defaultModelBuilder = buffers.get(this.defaultMaterial);

        this.prepareCulling(true);
        this.prepareAoInfo(model.useAmbientOcclusion());

        model.emitBlockQuads(this.world, state, pos, this.randomSupplier, this);
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

        this.colorizeQuad(quad, colorIndex);
        this.shadeQuad(quad, lightMode, emissive);
        this.bufferQuad(quad, this.quadLightData.br, material, modelBuilder);
    }

    private void colorizeQuad(MutableQuadViewImpl quad, int colorIndex) {
        if (colorIndex != -1) {
            ColorProvider<BlockState> colorProvider = this.colorProvider;

            if (colorProvider != null) {
                int[] vertexColors = this.vertexColors;
                colorProvider.getColors(this.slice, this.pos, this.state, quad, vertexColors);

                for (int i = 0; i < 4; i++) {
                    // Set alpha to 0xFF in case a quad transform inspects the color.
                    // We do not support per-vertex alpha, however, so this will get discarded at vertex encoding time.
                    quad.color(i, ColorHelper.multiplyColor(0xFF000000 | vertexColors[i], quad.color(i)));
                }
            }
        }
    }

    private void bufferQuad(MutableQuadViewImpl quad, float[] brightnesses, Material material, ChunkModelBuilder modelBuilder) {
        ModelQuadOrientation orientation = ModelQuadOrientation.orientByBrightness(brightnesses, quad);
        ChunkVertexEncoder.Vertex[] vertices = this.vertices;
        Vector3f offset = this.posOffset;

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = orientation.getVertexIndex(dstIndex);

            ChunkVertexEncoder.Vertex out = vertices[dstIndex];
            out.x = quad.x(srcIndex) + offset.x;
            out.y = quad.y(srcIndex) + offset.y;
            out.z = quad.z(srcIndex) + offset.z;

            // FRAPI uses ARGB color format; convert to ABGR.
            // Due to our vertex format, the alpha from the quad color is ignored entirely.
            out.color = ColorARGB.toABGR(quad.color(srcIndex), brightnesses[srcIndex]);

            out.u = quad.u(srcIndex);
            out.v = quad.v(srcIndex);

            out.light = quad.lightmap(srcIndex);
        }

        ModelQuadFacing normalFace = quad.normalFace();
        ChunkMeshBufferBuilder vertexBuffer = modelBuilder.getVertexBuffer(normalFace);
        vertexBuffer.push(vertices, material);

        modelBuilder.addSprite(quad.sprite(SpriteFinderCache.forBlockAtlas()));
    }
}
