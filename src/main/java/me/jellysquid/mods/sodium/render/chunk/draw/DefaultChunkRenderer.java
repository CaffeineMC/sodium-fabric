package me.jellysquid.mods.sodium.render.chunk.draw;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrays;
import me.jellysquid.mods.sodium.interop.vanilla.mixin.LightmapTextureManagerAccessor;
import me.jellysquid.mods.sodium.opengl.array.DrawCommandList;
import me.jellysquid.mods.sodium.opengl.device.RenderDevice;
import me.jellysquid.mods.sodium.opengl.pipeline.PipelineState;
import me.jellysquid.mods.sodium.opengl.types.IntType;
import me.jellysquid.mods.sodium.opengl.types.PrimitiveType;
import me.jellysquid.mods.sodium.render.buffer.VertexRange;
import me.jellysquid.mods.sodium.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;
import me.jellysquid.mods.sodium.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.render.chunk.state.UploadedChunkMesh;
import me.jellysquid.mods.sodium.render.sequence.SequenceBuilder;
import me.jellysquid.mods.sodium.render.sequence.SequenceIndexBuffer;
import me.jellysquid.mods.sodium.render.stream.MappedStreamingBuffer;
import me.jellysquid.mods.sodium.render.stream.StreamingBuffer;
import me.jellysquid.mods.sodium.render.terrain.format.TerrainVertexType;
import me.jellysquid.mods.sodium.render.terrain.quad.properties.ChunkMeshFace;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureManager;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.util.Map;

public class DefaultChunkRenderer extends ShaderChunkRenderer {
    private static final int INSTANCE_DATA_STRIDE = 16;
    private static final int COMMAND_DATA_SIZE = 20;

    private static final int MAX_COMMAND_BUFFER_SIZE = RenderRegion.REGION_SIZE * ChunkMeshFace.COUNT * COMMAND_DATA_SIZE;
    private static final int MAX_INSTANCE_DATA_SIZE = RenderRegion.REGION_SIZE * INSTANCE_DATA_STRIDE;

    private final StreamingBuffer commandBuffer;
    private final StreamingBuffer instanceDataBuffer;

    private final SequenceIndexBuffer indexBuffer;

    public DefaultChunkRenderer(RenderDevice device, TerrainVertexType vertexType) {
        super(device, vertexType);

        this.commandBuffer = new MappedStreamingBuffer(device, 16 * 1024 * 1024);
        this.instanceDataBuffer = new MappedStreamingBuffer(device, 4 * 1024 * 1024);
        this.indexBuffer = new SequenceIndexBuffer(device, SequenceBuilder.QUADS);
    }

    @Override
    public void render(ChunkRenderMatrices matrices, RenderDevice device,
                       ChunkRenderList list, ChunkRenderPass renderPass,
                       ChunkCameraContext camera) {
        var pipeline = this.getPipeline(renderPass);

        device.usePipeline(pipeline, (drawCommandList, programInterface, pipelineState) -> {
            this.bindTextures(renderPass, pipelineState);
            this.updateUniforms(matrices, programInterface);

            for (Map.Entry<RenderRegion, ObjectArrayList<RenderSection>> entry : sortedRegions(list, renderPass.usesReverseOrder())) {
                var region = entry.getKey();
                var sections = entry.getValue();

                var handles = this.prepareDrawBatches(sections, renderPass, camera, region.getResources());

                if (handles == null) {
                    continue;
                }

                this.executeDrawBatches(drawCommandList, programInterface, region.getResources(), handles);
            }
        });
    }

    private void bindTextures(ChunkRenderPass renderPass, PipelineState pipelineState) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextureManager textureManager = client.getTextureManager();

        LightmapTextureManagerAccessor lightmapTextureManager =
                ((LightmapTextureManagerAccessor) client.gameRenderer.getLightmapTextureManager());

        AbstractTexture blockAtlasTex = textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
        AbstractTexture lightTex = lightmapTextureManager.getTexture();

        pipelineState.bindTexture(0, blockAtlasTex.getGlId(), renderPass.mipped() ? this.blockTextureMippedSampler : this.blockTextureSampler);
        pipelineState.bindTexture(1, lightTex.getGlId(), this.lightTextureSampler);
    }

    private void updateUniforms(ChunkRenderMatrices matrices, ChunkShaderInterface programInterface) {
        programInterface.setFogUniforms();

        if (programInterface.uniformProjectionMatrix != null) {
            programInterface.uniformProjectionMatrix.set(matrices.projection());
        }

        if (programInterface.uniformModelViewMatrix != null) {
            programInterface.uniformModelViewMatrix.set(matrices.modelView());
        }

        if (programInterface.uniformModelViewProjectionMatrix != null) {
            var mvpMatrix = new Matrix4f();
            mvpMatrix.set(matrices.projection());
            mvpMatrix.mul(matrices.modelView());

            programInterface.uniformModelViewProjectionMatrix.set(mvpMatrix);
        }
    }

    private Handles prepareDrawBatches(ObjectArrayList<RenderSection> sections, ChunkRenderPass pass, ChunkCameraContext camera, RenderRegion.Resources regionResources) {
        var meshes = regionResources.getMeshes(pass);

        if (meshes == null) {
            return null;
        }

        var alignment = this.device.properties().uniformBufferOffsetAlignment;

        var commandBufferWriter = this.commandBuffer.write(MAX_COMMAND_BUFFER_SIZE, alignment);
        var instanceDataWriter = this.instanceDataBuffer.write(MAX_INSTANCE_DATA_SIZE, alignment);

        int instanceCount = 0;
        int drawCount = 0;

        int maxVertices = 0;

        for (RenderSection render : sortedChunks(sections, pass.usesReverseOrder())) {
            UploadedChunkMesh state = meshes[render.getChunkId()];

            if (state == null) {
                continue;
            }

            int visibilityFlags = render.getVisibilityFlags() & state.getVisibilityFlags();

            if (visibilityFlags != 0) {
                int baseVertex = state.getVertexSegment().getOffset();

                for (int face = 0; face < ChunkMeshFace.COUNT; face++) {
                    if ((visibilityFlags & (1 << face)) != 0) {
                        VertexRange range = state.getMeshPart(face);

                        if (range == null) {
                            throw new IllegalStateException("Invalid visibility flag state");
                        }

                        pushDrawCommand(commandBufferWriter, (range.vertexCount() / 4) * 6, 1, 0, baseVertex + range.firstVertex(), instanceCount);
                        drawCount++;
                        maxVertices = Math.max(maxVertices, range.vertexCount());
                    }
                }

                float x = getCameraTranslation(render.getOriginX(), camera.blockX, camera.deltaX);
                float y = getCameraTranslation(render.getOriginY(), camera.blockY, camera.deltaY);
                float z = getCameraTranslation(render.getOriginZ(), camera.blockZ, camera.deltaZ);

                pushInstanceData(instanceDataWriter, x, y, z);
                instanceCount++;
            }
        }

        var commandBufferHandle = commandBufferWriter.finish();
        var instanceDataHandle = instanceDataWriter.finish();

        if (commandBufferHandle == null || instanceDataHandle == null) {
            return null;
        }

        this.indexBuffer.ensureCapacity(maxVertices);

        return new Handles(commandBufferHandle, instanceDataHandle, instanceCount, drawCount);
    }

    private record Handles(StreamingBuffer.Handle commandBuffer, StreamingBuffer.Handle instanceData, int instanceCount, int drawCount) {
        public void free() {
            this.commandBuffer.free();
            this.instanceData.free();
        }
    }

    private static void pushInstanceData(StreamingBuffer.Writer writer,float x, float y, float z) {
        var ptr = writer.next(INSTANCE_DATA_STRIDE);
        MemoryUtil.memPutFloat(ptr + 0, x);
        MemoryUtil.memPutFloat(ptr + 4, y);
        MemoryUtil.memPutFloat(ptr + 8, z);
    }

    private static void pushDrawCommand(StreamingBuffer.Writer writer, int count, int instanceCount, int firstIndex, int baseVertex, int baseInstance) {
        var ptr = writer.next(COMMAND_DATA_SIZE);
        MemoryUtil.memPutInt(ptr + 0, count);
        MemoryUtil.memPutInt(ptr + 4, instanceCount);
        MemoryUtil.memPutInt(ptr + 8, firstIndex);
        MemoryUtil.memPutInt(ptr + 12, baseVertex);
        MemoryUtil.memPutInt(ptr + 16, baseInstance);
    }

    private void executeDrawBatches(DrawCommandList<BufferTarget> drawCommandList, ChunkShaderInterface programInterface, RenderRegion.Resources resources, Handles handles) {
        drawCommandList.bindVertexBuffer(BufferTarget.VERTICES, resources.vertexBuffers.getBufferObject(), 0, this.vertexFormat.getStride());
        drawCommandList.bindElementBuffer(this.indexBuffer.getBuffer());

        programInterface.bufferInstanceData.bindBuffer(handles.instanceData.getBuffer(), handles.instanceData.getOffset(), handles.instanceData.getLength());

        drawCommandList.multiDrawElementsIndirect(handles.commandBuffer.getBuffer(), handles.commandBuffer.getOffset(), handles.drawCount,
                IntType.UNSIGNED_INT, PrimitiveType.TRIANGLES);

        handles.free();
    }

    @Override
    public void delete() {
        super.delete();

        this.commandBuffer.delete();
        this.instanceDataBuffer.delete();
    }

    @Override
    public void flush() {
        this.commandBuffer.flush();
        this.instanceDataBuffer.flush();
    }

    private static Iterable<Map.Entry<RenderRegion, ObjectArrayList<RenderSection>>> sortedRegions(ChunkRenderList list, boolean translucent) {
        return list.sorted(translucent);
    }

    private static Iterable<RenderSection> sortedChunks(ObjectArrayList<RenderSection> chunks, boolean reverse) {
        if (reverse) {
            RenderSection[] copy = new RenderSection[chunks.size()];
            chunks.toArray(copy);

            ObjectArrays.reverse(copy);

            return ObjectArrayList.wrap(copy);
        }
        
        return chunks;
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }
}
