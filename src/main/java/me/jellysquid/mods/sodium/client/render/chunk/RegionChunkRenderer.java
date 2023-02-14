package me.jellysquid.mods.sodium.client.render.chunk;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.DrawCommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlIndexType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.gl.tessellation.TessellationBinding;
import me.jellysquid.mods.sodium.client.gl.util.ElementRange;
import me.jellysquid.mods.sodium.client.gl.device.MultiDrawBatch;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;

import java.util.List;

public class RegionChunkRenderer extends ShaderChunkRenderer {
    private final MultiDrawBatch batch;
    private final GlVertexAttributeBinding[] vertexAttributeBindings;

    private final boolean isBlockFaceCullingEnabled = SodiumClientMod.options().performance.useBlockFaceCulling;

    public RegionChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        super(device, vertexType);

        this.vertexAttributeBindings = new GlVertexAttributeBinding[] {
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.POSITION_MATERIAL_MESH)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.COLOR_SHADE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.BLOCK_TEXTURE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                        this.vertexFormat.getAttribute(ChunkMeshAttribute.LIGHT_TEXTURE))
        };

        this.batch = new MultiDrawBatch(ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE);
    }

    @Override
    public void render(ChunkRenderMatrices matrices, CommandList commandList,
                       RenderRegionManager regions, ChunkRenderList list, TerrainRenderPass pass,
                       ChunkCameraContext camera) {
        super.begin(pass);

        ChunkShaderInterface shader = this.activeProgram.getInterface();
        shader.setProjectionMatrix(matrices.projection());
        shader.setModelViewMatrix(matrices.modelView());

        for (Long2ReferenceMap.Entry<List<RenderSection>> entry : sortedRegions(list, pass.isReverseOrder())) {
            var region = regions.getRegion(entry.getLongKey());
            var sections = entry.getValue();

            var storage = region.getStorage(pass);

            if (storage == null) {
                continue;
            }

            var batch = this.buildDrawBatches(storage, sections, pass, camera);

            if (!batch.isEmpty()) {
                this.setModelMatrixUniforms(shader, region, camera);
                this.executeDrawBatch(commandList, this.createTessellationForRegion(commandList, region, pass));
            }
        }

        super.end(pass);
    }

    private MultiDrawBatch buildDrawBatches(RenderRegion.RenderRegionStorage storage, List<RenderSection> sections, TerrainRenderPass pass, ChunkCameraContext camera) {
        var batch = this.batch;
        batch.clear();

        for (RenderSection section : sortedChunks(sections, pass.isReverseOrder())) {
            ChunkGraphicsState state = storage.getState(section);

            if (state == null) {
                continue;
            }

            long indexOffset = state.getIndexSegment()
                    .getOffset() * Integer.BYTES;

            int baseVertex = state.getVertexSegment()
                    .getOffset();

            this.addDrawCalls(camera, section, state, indexOffset, baseVertex);
        }

        return batch;
    }

    private void addDrawCalls(ChunkCameraContext camera, RenderSection section, ChunkGraphicsState state, long indexOffset, int baseVertex) {
        this.addDrawCall(state.getModelPart(ModelQuadFacing.UNASSIGNED), indexOffset, baseVertex);

        if (this.isBlockFaceCullingEnabled) {
            ChunkRenderBounds bounds = section.getBounds();

            if (camera.posY > bounds.y1) {
                this.addDrawCall(state.getModelPart(ModelQuadFacing.UP), indexOffset, baseVertex);
            }

            if (camera.posY < bounds.y2) {
                this.addDrawCall(state.getModelPart(ModelQuadFacing.DOWN), indexOffset, baseVertex);
            }

            if (camera.posX > bounds.x1) {
                this.addDrawCall(state.getModelPart(ModelQuadFacing.EAST), indexOffset, baseVertex);
            }

            if (camera.posX < bounds.x2) {
                this.addDrawCall(state.getModelPart(ModelQuadFacing.WEST), indexOffset, baseVertex);
            }

            if (camera.posZ > bounds.z1) {
                this.addDrawCall(state.getModelPart(ModelQuadFacing.SOUTH), indexOffset, baseVertex);
            }

            if (camera.posZ < bounds.z2) {
                this.addDrawCall(state.getModelPart(ModelQuadFacing.NORTH), indexOffset, baseVertex);
            }
        } else {
            for (ModelQuadFacing facing : ModelQuadFacing.DIRECTIONS) {
                this.addDrawCall(state.getModelPart(facing), indexOffset, baseVertex);
            }
        }
    }

    private GlTessellation createTessellationForRegion(CommandList commandList, RenderRegion region, TerrainRenderPass pass) {
        var storage = region.getStorage(pass);
        var tessellation = storage.getTessellation();

        if (tessellation == null) {
            storage.setTessellation(tessellation = this.createRegionTessellation(commandList, region));
        }

        return tessellation;
    }

    private void executeDrawBatch(CommandList commandList, GlTessellation tessellation) {
        MultiDrawBatch batch = this.batch;

        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsBaseVertex(batch, GlIndexType.UNSIGNED_INT);
        }
    }

    private void setModelMatrixUniforms(ChunkShaderInterface shader, RenderRegion region, ChunkCameraContext camera) {
        float x = getCameraTranslation(region.getOriginX(), camera.blockX, camera.deltaX);
        float y = getCameraTranslation(region.getOriginY(), camera.blockY, camera.deltaY);
        float z = getCameraTranslation(region.getOriginZ(), camera.blockZ, camera.deltaZ);

        shader.setRegionOffset(x, y, z);
    }

    private void addDrawCall(ElementRange part, long baseIndexPointer, int baseVertexIndex) {
        if (part != null) {
            this.batch.add(baseIndexPointer + part.elementPointer(), part.elementCount(), baseVertexIndex);
        }
    }

    private GlTessellation createRegionTessellation(CommandList commandList, RenderRegion region) {
        return commandList.createTessellation(GlPrimitiveType.TRIANGLES, new TessellationBinding[] {
                TessellationBinding.forVertexBuffer(region.vertexBuffers.getBufferObject(), this.vertexAttributeBindings),
                TessellationBinding.forElementBuffer(region.indexBuffers.getBufferObject())
        });
    }

    @Override
    public void delete() {
        super.delete();

        this.batch.delete();
    }

    private static Iterable<Long2ReferenceMap.Entry<List<RenderSection>>> sortedRegions(ChunkRenderList list, boolean translucent) {
        return list.sorted(translucent);
    }

    private static Iterable<RenderSection> sortedChunks(List<RenderSection> chunks, boolean translucent) {
        return translucent ? Lists.reverse(chunks) : chunks;
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }
}
