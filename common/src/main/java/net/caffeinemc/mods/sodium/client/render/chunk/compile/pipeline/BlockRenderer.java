package net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline;

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.frapi.helper.ColorHelper;
import net.caffeinemc.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.caffeinemc.mods.sodium.client.services.PlatformModelAccess;
import net.caffeinemc.mods.sodium.client.services.SodiumModelData;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class BlockRenderer extends AbstractBlockRenderContext {
    private final ColorProviderRegistry colorProviderRegistry;
    private final int[] vertexColors = new int[4];
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private ChunkBuildBuffers buffers;

    private final Vector3f posOffset = new Vector3f();
    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    @Nullable
    private ColorProvider<BlockState> colorProvider;
    private TranslucentGeometryCollector collector;

    public BlockRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lighters) {
        this.colorProviderRegistry = colorRegistry;
        this.lighters = lighters;

        this.random = new SingleThreadedRandomSource(42L);
    }

    public void prepare(ChunkBuildBuffers buffers, LevelSlice level, TranslucentGeometryCollector collector) {
        this.buffers = buffers;
        this.level = level;
        this.collector = collector;
        this.slice = level;
    }

    public void release() {
        this.buffers = null;
        this.level = null;
        this.collector = null;
        this.slice = null;
    }

    public void renderModel(BakedModel model, BlockState state, BlockPos pos, BlockPos origin) {
        this.state = state;
        this.pos = pos;

        this.randomSeed = state.getSeed(pos);

        this.posOffset.set(origin.getX(), origin.getY(), origin.getZ());
        if (state.hasOffsetFunction()) {
            Vec3 modelOffset = state.getOffset(this.level, pos);
            this.posOffset.add((float) modelOffset.x, (float) modelOffset.y, (float) modelOffset.z);
        }

        this.colorProvider = this.colorProviderRegistry.getColorProvider(state.getBlock());

        type = ItemBlockRenderTypes.getChunkRenderType(state);

        this.prepareCulling(true);
        this.prepareAoInfo(model.useAmbientOcclusion());

        modelData = PlatformModelAccess.getInstance().getModelData(slice, model, state, pos, slice.getPlatformModelData(pos));

        Iterable<RenderType> renderTypes = PlatformModelAccess.getInstance().getModelRenderTypes(level, model, state, pos, random, modelData);

        for (RenderType type : renderTypes) {
            this.type = type;
            ((FabricBakedModel) model).emitBlockQuads(this.level, state, pos, this.randomSupplier, this);
        }

        type = null;
        modelData = SodiumModelData.EMPTY;
    }

    /**
     * Process quad, after quad transforms and the culling check have been applied.
     */
    @Override
    protected void processQuad(MutableQuadViewImpl quad) {
        final RenderMaterial mat = quad.material();
        final int colorIndex = mat.disableColorIndex() ? -1 : quad.colorIndex();
        final TriState aoMode = mat.ambientOcclusion();
        final ShadeMode shadeMode = mat.shadeMode();
        final LightMode lightMode;
        if (aoMode == TriState.DEFAULT) {
            lightMode = this.defaultLightMode;
        } else {
            lightMode = this.useAmbientOcclusion && aoMode.get() ? LightMode.SMOOTH : LightMode.FLAT;
        }
        final boolean emissive = mat.emissive();

        Material material;

        final BlendMode blendMode = mat.blendMode();
        if (blendMode == BlendMode.DEFAULT) {
            material = DefaultMaterials.forRenderLayer(type);
        } else {
            material = DefaultMaterials.forRenderLayer(blendMode.blockRenderLayer == null ? type : blendMode.blockRenderLayer);
        }

        this.colorizeQuad(quad, colorIndex);
        this.shadeQuad(quad, lightMode, emissive, shadeMode);
        this.bufferQuad(quad, this.quadLightData.br, material);
    }

    private void colorizeQuad(MutableQuadViewImpl quad, int colorIndex) {
        if (colorIndex != -1) {
            ColorProvider<BlockState> colorProvider = this.colorProvider;

            if (colorProvider != null) {
                int[] vertexColors = this.vertexColors;
                colorProvider.getColors(this.slice, this.pos, this.scratchPos, this.state, quad, vertexColors);

                for (int i = 0; i < 4; i++) {
                    // Set alpha to 0xFF in case a quad transform inspects the color.
                    // We do not support per-vertex alpha, however, so this will get discarded at vertex encoding time.
                    quad.color(i, ColorHelper.multiplyColor(0xFF000000 | vertexColors[i], quad.color(i)));
                }
            }
        }
    }

    private void bufferQuad(MutableQuadViewImpl quad, float[] brightnesses, Material material) {
        ModelQuadOrientation orientation = defaultLightMode == LightMode.FLAT ? ModelQuadOrientation.NORMAL : ModelQuadOrientation.orientByBrightness(brightnesses, quad);
        ChunkVertexEncoder.Vertex[] vertices = this.vertices;
        Vector3f offset = this.posOffset;

        float minU = 1;
        float minV = 1;
        float maxU = 0;
        float maxV = 0;

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = orientation.getVertexIndex(dstIndex);

            ChunkVertexEncoder.Vertex out = vertices[dstIndex];
            out.x = quad.x(srcIndex) + offset.x;
            out.y = quad.y(srcIndex) + offset.y;
            out.z = quad.z(srcIndex) + offset.z;

            // FRAPI uses ARGB color format; convert to ABGR.
            // Due to our vertex format, the alpha from the quad color is ignored entirely.
            out.color = ColorARGB.toABGR(quad.color(srcIndex), brightnesses[srcIndex]);

            var u = quad.u(srcIndex);
            var v = quad.v(srcIndex);
            out.u = u;
            out.v = v;
            minU = Math.min(minU, u);
            minV = Math.min(minV, v);
            maxU = Math.max(maxU, u);
            maxV = Math.max(maxV, v);

            out.light = quad.lightmap(srcIndex);
        }

        // TODO: actually use the uv bounding box to check each quad
        var atlasSprite = SpriteFinderCache.forBlockAtlas().find(quad.getTexU(0), quad.getTexV(0));
        material = getDowngradedMaterial(atlasSprite, material);

        ModelQuadFacing normalFace = quad.normalFace();

        if (material.isTranslucent() && this.collector != null) {
            this.collector.appendQuad(quad.getFaceNormal(), vertices, normalFace);
        }

        ChunkModelBuilder builder = this.buffers.get(material);

        ChunkMeshBufferBuilder vertexBuffer = builder.getVertexBuffer(normalFace);
        vertexBuffer.push(vertices, material);

        builder.addSprite(atlasSprite);
    }

    private Material getDowngradedMaterial(TextureAtlasSprite sprite, Material material) {
        var contents = (SpriteContentsExtension)(sprite.contents());
        if (material.pass == DefaultTerrainRenderPasses.TRANSLUCENT && !contents.sodium$hasTranslucentPixels()) {
            material = DefaultMaterials.CUTOUT_MIPPED;
        }
        if (material.pass == DefaultTerrainRenderPasses.CUTOUT && !contents.sodium$hasTransparentPixels()) {
            material = DefaultMaterials.SOLID;
        }
        return material;
    }
}