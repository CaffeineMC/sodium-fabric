package me.jellysquid.mods.sodium.client.render.renderer;

import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.light.QuadLighter;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.material.MaterialCutoutFlag;
import me.jellysquid.mods.sodium.client.render.chunk.material.MaterialFlag;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.interop.fabric.helper.GeometryHelper;
import me.jellysquid.mods.sodium.client.interop.fabric.mesh.MutableQuadViewImpl;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.math.Vec3i;

public class TerrainRenderer extends AbstractRenderer<TerrainBlockRenderInfo> {
    private final ChunkBuildBuffers buildBuffers;
    
    TerrainRenderer(TerrainBlockRenderInfo blockInfo, ChunkBuildBuffers buildBuffers, QuadLighter lighter, RenderContext.QuadTransform transform) {
        super(blockInfo, lighter, transform);
        
        this.buildBuffers = buildBuffers;
    }

    @Override
    protected void emitQuad(MutableQuadViewImpl quad, BlendMode renderLayer) {
        RenderLayer layer = renderLayer.blockRenderLayer;

        int bits = switch (renderLayer) {
            case SOLID, DEFAULT, TRANSLUCENT -> MaterialCutoutFlag.shift(MaterialCutoutFlag.NONE);
            case CUTOUT -> MaterialCutoutFlag.shift(MaterialCutoutFlag.TENTH) | MaterialFlag.CUTOUT;
            case CUTOUT_MIPPED -> MaterialCutoutFlag.shift(MaterialCutoutFlag.HALF);
        };

        ChunkModelBuilder builder = this.buildBuffers.getBuilder(layer);

        ModelVertexSink vertexSink = builder.getVertexSink();
        vertexSink.ensureCapacity(4);

        IndexBufferBuilder indexSink = builder.getIndexBufferBuilder(getBlockFace(quad));
        int vertexStart = vertexSink.getVertexCount();

        Vec3i relativePos = this.blockInfo.getRelativeBlockPosition();
        int chunkId = this.blockInfo.getChunkId();

        for (int i = 0; i < 4; i++) {
            vertexSink.writeVertex(quad.x(i) + relativePos.getX(), quad.y(i) + relativePos.getY(), quad.z(i) + relativePos.getZ(),
                    ColorARGB.toABGR(quad.spriteColor(i, 0)),
                    quad.spriteU(i, 0), quad.spriteV(i, 0),
                    quad.lightmap(i),
                    chunkId,
                    bits);
        }

        indexSink.add(vertexStart, ModelQuadWinding.CLOCKWISE);

        vertexSink.flush();
    }

    private static ModelQuadFacing getBlockFace(MutableQuadViewImpl quad) {
        // Model quads which are not aligned to the world grid are not eligible for plane culling
        if (quad.nominalFace() == null || (quad.geometryFlags() & GeometryHelper.AXIS_ALIGNED_FLAG) == 0) {
            return ModelQuadFacing.UNASSIGNED;
        }

        return ModelQuadFacing.fromDirection(quad.nominalFace());
    }
}
