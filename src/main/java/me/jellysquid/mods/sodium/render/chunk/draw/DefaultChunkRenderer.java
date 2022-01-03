package me.jellysquid.mods.sodium.render.chunk.draw;

import com.google.common.collect.Lists;
import me.jellysquid.mods.sodium.SodiumClientMod;
import me.jellysquid.mods.sodium.opengl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.opengl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.opengl.device.CommandList;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.array.*;
import me.jellysquid.mods.sodium.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.render.chunk.buffer.ElementRange;
import me.jellysquid.mods.sodium.render.chunk.state.UploadedChunkMesh;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexType;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkRenderBounds;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainMeshAttribute;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;
import me.jellysquid.mods.sodium.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.util.draw.MultiDrawBatch;

import java.util.List;
import java.util.Map;

public class DefaultChunkRenderer extends ShaderChunkRenderer {
    private final MultiDrawBatch[] batches;

    private final GlBuffer chunkInfoBuffer;
    private final boolean isBlockFaceCullingEnabled = SodiumClientMod.options().performance.useBlockFaceCulling;

    private final VertexArray<BufferTarget> vertexArray;

    public DefaultChunkRenderer(RenderDevice device, TerrainVertexType vertexType) {
        super(device, vertexType);

        try (CommandList commandList = device.createCommandList()) {
            this.chunkInfoBuffer = commandList.createBuffer(RenderRegion.REGION_SIZE * 16, (buffer) -> {
                for (int x = 0; x < RenderRegion.REGION_WIDTH; x++) {
                    for (int y = 0; y < RenderRegion.REGION_HEIGHT; y++) {
                        for (int z = 0; z < RenderRegion.REGION_LENGTH; z++) {
                            int offset = RenderRegion.getChunkIndex(x, y, z) * 16;

                            buffer.putFloat(offset + 0, x * 16.0f);
                            buffer.putFloat(offset + 4, y * 16.0f);
                            buffer.putFloat(offset + 8, z * 16.0f);
                        }
                    }
                }
            });

            this.vertexArray = commandList.createVertexArray(new VertexArrayDescription<>(BufferTarget.class, List.of(
                    new VertexBufferBinding<>(BufferTarget.VERTICES, new GlVertexAttributeBinding[] {
                            new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID,
                                    this.vertexFormat.getAttribute(TerrainMeshAttribute.POSITION_ID)),
                            new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                                    this.vertexFormat.getAttribute(TerrainMeshAttribute.COLOR)),
                            new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                                    this.vertexFormat.getAttribute(TerrainMeshAttribute.BLOCK_TEXTURE)),
                            new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                                    this.vertexFormat.getAttribute(TerrainMeshAttribute.LIGHT_TEXTURE))
                    })
            )));
        }

        this.batches = new MultiDrawBatch[GlIndexType.VALUES.length];

        for (int i = 0; i < this.batches.length; i++) {
            this.batches[i] = MultiDrawBatch.create(ChunkMeshFace.COUNT * RenderRegion.REGION_SIZE);
        }
    }

    @Override
    public void render(ChunkRenderMatrices matrices, CommandList commandList,
                       ChunkRenderList list, ChunkRenderPass pass,
                       ChunkCameraContext camera) {
        super.begin(pass);

        ChunkShaderInterface shader = this.activeProgram.getInterface();
        shader.setProjectionMatrix(matrices.projection());
        shader.setModelViewMatrix(matrices.modelView());

        shader.setDrawUniforms(this.chunkInfoBuffer);

        commandList.useVertexArray(this.vertexArray, (drawCommandList) -> {
            for (Map.Entry<RenderRegion, List<RenderSection>> entry : sortedRegions(list, pass.isTranslucent())) {
                RenderRegion region = entry.getKey();
                List<RenderSection> regionSections = entry.getValue();

                if (!this.buildDrawBatches(regionSections, pass, camera)) {
                    continue;
                }

                this.setModelMatrixUniforms(shader, region, camera);
                this.executeDrawBatches(drawCommandList, region.getArenas());
            }
        });
        
        super.end();
    }

    private boolean buildDrawBatches(List<RenderSection> sections, ChunkRenderPass pass, ChunkCameraContext camera) {
        for (MultiDrawBatch batch : this.batches) {
            batch.begin();
        }

        for (RenderSection render : sortedChunks(sections, pass.isTranslucent())) {
            UploadedChunkMesh state = render.getMesh(pass);

            if (state == null) {
                continue;
            }

            ChunkRenderBounds bounds = render.getBounds();

            long indexOffset = state.getIndexSegment()
                    .getOffset();

            int baseVertex = state.getVertexSegment()
                    .getOffset() / this.vertexFormat.getStride();

            this.addDrawCall(state.getMeshPart(ChunkMeshFace.UNASSIGNED), indexOffset, baseVertex);

            if (this.isBlockFaceCullingEnabled) {
                if (camera.posY > bounds.y1) {
                    this.addDrawCall(state.getMeshPart(ChunkMeshFace.UP), indexOffset, baseVertex);
                }

                if (camera.posY < bounds.y2) {
                    this.addDrawCall(state.getMeshPart(ChunkMeshFace.DOWN), indexOffset, baseVertex);
                }

                if (camera.posX > bounds.x1) {
                    this.addDrawCall(state.getMeshPart(ChunkMeshFace.EAST), indexOffset, baseVertex);
                }

                if (camera.posX < bounds.x2) {
                    this.addDrawCall(state.getMeshPart(ChunkMeshFace.WEST), indexOffset, baseVertex);
                }

                if (camera.posZ > bounds.z1) {
                    this.addDrawCall(state.getMeshPart(ChunkMeshFace.SOUTH), indexOffset, baseVertex);
                }

                if (camera.posZ < bounds.z2) {
                    this.addDrawCall(state.getMeshPart(ChunkMeshFace.NORTH), indexOffset, baseVertex);
                }
            } else {
                for (ChunkMeshFace facing : ChunkMeshFace.DIRECTIONS) {
                    this.addDrawCall(state.getMeshPart(facing), indexOffset, baseVertex);
                }
            }
        }

        boolean nonEmpty = false;

        for (MultiDrawBatch batch : this.batches) {
            batch.end();

            nonEmpty |= !batch.isEmpty();
        }

        return nonEmpty;
    }

    private void executeDrawBatches(VertexArrayCommandList<BufferTarget> drawCommandList, RenderRegion.RenderRegionArenas arenas) {
        drawCommandList.bindVertexBuffers(this.vertexArray.createBindings(
                Map.of(BufferTarget.VERTICES, new VertexArrayBuffer(arenas.vertexBuffers.getBufferObject(), this.vertexFormat.getStride()))
        ));
        drawCommandList.bindElementBuffer(arenas.indexBuffers.getBufferObject());

        for (int i = 0; i < this.batches.length; i++) {
            MultiDrawBatch batch = this.batches[i];

            if (batch.isEmpty()) {
                continue;
            }

            drawCommandList.multiDrawElementsBaseVertex(batch.getPointerBuffer(), batch.getCountBuffer(), batch.getBaseVertexBuffer(),
                    GlIndexType.VALUES[i], GlPrimitiveType.TRIANGLES);
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
            MultiDrawBatch batch = this.batches[part.indexType().ordinal()];
            batch.add(baseIndexPointer + part.elementPointer(), part.elementCount(), baseVertexIndex + part.baseVertex());
        }
    }

    @Override
    public void delete() {
        super.delete();

        for (MultiDrawBatch batch : this.batches) {
            batch.delete();
        }

        RenderDevice.INSTANCE.createCommandList()
                .deleteBuffer(this.chunkInfoBuffer);
    }

    private static Iterable<Map.Entry<RenderRegion, List<RenderSection>>> sortedRegions(ChunkRenderList list, boolean translucent) {
        return list.sorted(translucent);
    }

    private static Iterable<RenderSection> sortedChunks(List<RenderSection> chunks, boolean translucent) {
        return translucent ? Lists.reverse(chunks) : chunks;
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }

    private enum BufferTarget {
        VERTICES
    }
}
