package me.jellysquid.mods.sodium.render.chunk.draw;

import com.google.common.collect.Lists;
import me.jellysquid.mods.sodium.SodiumClientMod;
import me.jellysquid.mods.sodium.interop.vanilla.mixin.LightmapTextureManagerAccessor;
import me.jellysquid.mods.sodium.opengl.array.*;
import me.jellysquid.mods.sodium.opengl.attribute.VertexAttributeBinding;
import me.jellysquid.mods.sodium.opengl.buffer.Buffer;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.types.IntType;
import me.jellysquid.mods.sodium.opengl.types.PrimitiveType;
import me.jellysquid.mods.sodium.render.buffer.ElementRange;
import me.jellysquid.mods.sodium.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;
import me.jellysquid.mods.sodium.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkRenderBounds;
import me.jellysquid.mods.sodium.render.chunk.state.UploadedChunkMesh;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainMeshAttribute;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexType;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import me.jellysquid.mods.sodium.util.draw.MultiDrawBatch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;

import java.util.List;
import java.util.Map;

public class DefaultChunkRenderer extends ShaderChunkRenderer {
    private final MultiDrawBatch[] batches;

    private final Buffer chunkInfoBuffer;
    private final boolean isBlockFaceCullingEnabled = SodiumClientMod.options().performance.useBlockFaceCulling;

    private final VertexArray<BufferTarget> vertexArray;

    public DefaultChunkRenderer(RenderDevice device, TerrainVertexType vertexType) {
        super(device, vertexType);

        this.chunkInfoBuffer = device.createBuffer(RenderRegion.REGION_SIZE * 16, (buffer) -> {
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

        this.vertexArray = device.createVertexArray(new VertexArrayDescription<>(BufferTarget.class, List.of(
                new VertexArrayResourceBinding<>(BufferTarget.VERTICES, new VertexAttributeBinding[] {
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID,
                                this.vertexFormat.getAttribute(TerrainMeshAttribute.POSITION_ID)),
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                                this.vertexFormat.getAttribute(TerrainMeshAttribute.COLOR)),
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                                this.vertexFormat.getAttribute(TerrainMeshAttribute.BLOCK_TEXTURE)),
                        new VertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                                this.vertexFormat.getAttribute(TerrainMeshAttribute.LIGHT_TEXTURE))
                })
        )));

        this.batches = new MultiDrawBatch[IntType.VALUES.length];

        for (int i = 0; i < this.batches.length; i++) {
            this.batches[i] = MultiDrawBatch.create(ChunkMeshFace.COUNT * RenderRegion.REGION_SIZE);
        }
    }

    @Override
    public void render(ChunkRenderMatrices matrices, RenderDevice device,
                       ChunkRenderList list, ChunkRenderPass renderPass,
                       ChunkCameraContext camera) {
        var options = new ChunkShaderOptions(renderPass, this.vertexType);
        var program = this.compileProgram(options);

        MinecraftClient client = MinecraftClient.getInstance();
        TextureManager textureManager = client.getTextureManager();

        LightmapTextureManagerAccessor lightmapTextureManager =
                ((LightmapTextureManagerAccessor) client.gameRenderer.getLightmapTextureManager());

        AbstractTexture blockAtlasTex = textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        AbstractTexture lightTex = lightmapTextureManager.getTexture();

        device.usePipeline(renderPass.pipeline(), (pipelineCommands, pipelineState) -> {
            pipelineState.bindTexture(0, blockAtlasTex.getGlId(), renderPass.mipped() ? this.blockTextureMippedSampler : this.blockTextureSampler);
            pipelineState.bindTexture(1, lightTex.getGlId(), this.lightTextureSampler);

            pipelineCommands.useProgram(program, (programCommands, programInterface) -> {
                programInterface.setup();
                programInterface.uniformProjectionMatrix.set(matrices.projection());
                programInterface.uniformModelViewMatrix.set(matrices.modelView());
                programInterface.uniformBlockDrawParameters.bindBuffer(this.chunkInfoBuffer);

                programCommands.useVertexArray(this.vertexArray, (drawCommandList) -> {
                    for (Map.Entry<RenderRegion, List<RenderSection>> entry : sortedRegions(list, renderPass.isTranslucent())) {
                        RenderRegion region = entry.getKey();
                        List<RenderSection> regionSections = entry.getValue();

                        if (!this.buildDrawBatches(regionSections, renderPass, camera)) {
                            continue;
                        }

                        this.setModelMatrixUniforms(programInterface, region, camera);
                        this.executeDrawBatches(drawCommandList, region.getArenas());
                    }
                });
            });
        });
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
        drawCommandList.bindVertexBuffers(this.vertexArray.createResourceSet(
                Map.of(BufferTarget.VERTICES, new VertexArrayBuffer(arenas.vertexBuffers.getBufferObject(), this.vertexFormat.getStride()))
        ));
        drawCommandList.bindElementBuffer(arenas.indexBuffers.getBufferObject());

        for (int i = 0; i < this.batches.length; i++) {
            MultiDrawBatch batch = this.batches[i];

            if (batch.isEmpty()) {
                continue;
            }

            drawCommandList.multiDrawElementsBaseVertex(batch.getPointerBuffer(), batch.getCountBuffer(), batch.getBaseVertexBuffer(),
                    IntType.VALUES[i], PrimitiveType.TRIANGLES);
        }
    }

    private void setModelMatrixUniforms(ChunkShaderInterface shader, RenderRegion region, ChunkCameraContext camera) {
        float x = getCameraTranslation(region.getOriginX(), camera.blockX, camera.deltaX);
        float y = getCameraTranslation(region.getOriginY(), camera.blockY, camera.deltaY);
        float z = getCameraTranslation(region.getOriginZ(), camera.blockZ, camera.deltaZ);

        shader.uniformRegionOffset.setFloats(x, y, z);
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

        this.device.deleteBuffer(this.chunkInfoBuffer);
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

    public enum BufferTarget {
        VERTICES
    }
}
