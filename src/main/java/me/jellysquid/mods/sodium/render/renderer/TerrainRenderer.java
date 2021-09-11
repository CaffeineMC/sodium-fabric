package me.jellysquid.mods.sodium.render.renderer;

import me.jellysquid.mods.sodium.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.model.light.QuadLighter;
import me.jellysquid.mods.sodium.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.render.chunk.format.ModelVertexCompression;
import me.jellysquid.mods.sodium.render.chunk.material.MaterialCutoutFlag;
import me.jellysquid.mods.sodium.render.chunk.material.MaterialFlag;
import me.jellysquid.mods.sodium.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.interop.fabric.helper.GeometryHelper;
import me.jellysquid.mods.sodium.interop.fabric.mesh.MutableQuadViewImpl;
import me.jellysquid.mods.sodium.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.render.chunk.passes.DefaultBlockRenderPasses;
import me.jellysquid.mods.sodium.util.color.ColorARGB;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.math.Vec3i;

public class TerrainRenderer extends AbstractRenderer<TerrainBlockRenderInfo> {
    private final ChunkBuildBuffers buildBuffers;
    private final SpriteFinder spriteFinder = SpriteFinder.get(MinecraftClient.getInstance()
            .getBakedModelManager()
            .getAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE));

    TerrainRenderer(TerrainBlockRenderInfo blockInfo, ChunkBuildBuffers buildBuffers, QuadLighter lighter, RenderContext.QuadTransform transform) {
        super(blockInfo, lighter, transform);
        
        this.buildBuffers = buildBuffers;
    }

    @Override
    protected void emitQuad(MutableQuadViewImpl quad, BlendMode renderLayer) {
        BlockRenderPass pass = switch (renderLayer) {
            case DEFAULT, SOLID -> DefaultBlockRenderPasses.SOLID;
            case CUTOUT, CUTOUT_MIPPED -> DefaultBlockRenderPasses.CUTOUT;
            case TRANSLUCENT -> DefaultBlockRenderPasses.TRANSLUCENT;
        };

        int bits = switch (renderLayer) {
            case SOLID, DEFAULT, TRANSLUCENT -> MaterialCutoutFlag.shift(MaterialCutoutFlag.NONE);
            case CUTOUT -> MaterialCutoutFlag.shift(MaterialCutoutFlag.TENTH) | MaterialFlag.CUTOUT;
            case CUTOUT_MIPPED -> MaterialCutoutFlag.shift(MaterialCutoutFlag.HALF);
        };

        ChunkModelBuilder builder = this.buildBuffers.getBuilder(pass);

        ModelVertexSink vertexSink = builder.getVertexSink();
        vertexSink.ensureCapacity(4);

        IndexBufferBuilder indexSink = builder.getIndexSink(getBlockFace(quad));
        int vertexStart = vertexSink.getVertexCount();

        Vec3i relativePos = this.blockInfo.getRelativeBlockPosition();
        short chunkId = this.blockInfo.getChunkId();

        for (int i = 0; i < 4; i++) {
            long pos = ModelVertexCompression.encodePositionAttribute(
                    quad.x(i) + relativePos.getX(), quad.y(i) + relativePos.getY(), quad.z(i) + relativePos.getZ());

            int color = ColorARGB.toABGR(quad.spriteColor(i, DEFAULT_TEXTURE_INDEX));
            int blockTexture = ModelVertexCompression.encodeTextureAttribute(
                    quad.spriteU(i, DEFAULT_TEXTURE_INDEX), quad.spriteV(i, DEFAULT_TEXTURE_INDEX));

            int lightTexture = ModelVertexCompression.encodeLightMapTexCoord(quad.lightmap(i));

            vertexSink.writeVertex(pos, color, blockTexture, lightTexture, chunkId, bits);
        }

        indexSink.add(vertexStart, ModelQuadWinding.CLOCKWISE);
        vertexSink.flush();

        Sprite sprite = this.spriteFinder.find(quad, DEFAULT_TEXTURE_INDEX);

        if (sprite != null) {
            builder.addSprite(sprite);
        }
    }

    private static ModelQuadFacing getBlockFace(MutableQuadViewImpl quad) {
        // Model quads which are not aligned to the world grid are not eligible for plane culling
        if (quad.nominalFace() == null || (quad.geometryFlags() & GeometryHelper.AXIS_ALIGNED_FLAG) == 0) {
            return ModelQuadFacing.UNASSIGNED;
        }

        return ModelQuadFacing.fromDirection(quad.nominalFace());
    }
}
