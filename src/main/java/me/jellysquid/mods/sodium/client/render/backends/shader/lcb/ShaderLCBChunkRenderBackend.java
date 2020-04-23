package me.jellysquid.mods.sodium.client.render.backends.shader.lcb;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.BufferUploadData;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMesh;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import net.minecraft.client.util.GlAllocationUtils;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ShaderLCBChunkRenderBackend extends AbstractShaderChunkRenderBackend<ShaderLCBRenderState> {
    private final ChunkBufferManager bufferManager;
    private final GlMutableBuffer uploadBuffer;

    private final IntBuffer bufIndices;
    private final IntBuffer bufLen;

    private final Reference2ObjectMap<BufferBlock, List<ShaderLCBRenderState>> lists = new Reference2ObjectOpenHashMap<>();

    public ShaderLCBChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);

        this.bufferManager = new ChunkBufferManager(this.useImmutableStorage);

        int maxBatchSize = ChunkBufferManager.getMaxBatchSize();

        this.bufIndices = GlAllocationUtils.allocateByteBuffer(maxBatchSize * 4).asIntBuffer();
        this.bufLen = GlAllocationUtils.allocateByteBuffer(maxBatchSize * 4).asIntBuffer();

        this.uploadBuffer = new GlMutableBuffer(GL15.GL_STREAM_COPY);
    }

    @Override
    public void upload(Iterator<ChunkBuildResult<ShaderLCBRenderState>> queue) {
        GlMutableBuffer uploadBuffer = this.uploadBuffer;
        uploadBuffer.bind(GL15.GL_ARRAY_BUFFER);

        BufferBlock prevBlock = null;

        while (queue.hasNext()) {
            ChunkBuildResult<ShaderLCBRenderState> result = queue.next();

            ChunkRender<ShaderLCBRenderState> render = result.render;
            ChunkRenderData data = result.data;

            render.resetRenderStates();
            render.setData(data);

            for (ChunkMesh mesh : data.getMeshes()) {
                ChunkSectionPos pos = render.getChunkPos();
                BufferBlock block = this.bufferManager.getOrCreateBlock(pos);

                if (prevBlock != block) {
                    if (prevBlock != null) {
                        prevBlock.endUploads();
                    }

                    block.beginUpload();

                    prevBlock = block;
                }

                BufferUploadData upload = mesh.takePendingUpload();
                uploadBuffer.upload(GL15.GL_ARRAY_BUFFER, upload);

                BufferSegment segment = block.upload(GL15.GL_ARRAY_BUFFER, 0, upload.buffer.capacity());

                render.setRenderState(mesh.getRenderPass(), new ShaderLCBRenderState(segment));
            }
        }

        if (prevBlock != null) {
            prevBlock.endUploads();
        }

        uploadBuffer.invalidate(GL15.GL_ARRAY_BUFFER);
        uploadBuffer.unbind(GL15.GL_ARRAY_BUFFER);
    }

    @Override
    public void render(Iterator<ShaderLCBRenderState> renders, MatrixStack matrixStack, double x, double y, double z) {
        this.bufferManager.cleanup();

        this.setupLists(renders);
        this.begin(matrixStack);

        int chunkX = (int) (x / 16.0D);
        int chunkY = (int) (y / 16.0D);
        int chunkZ = (int) (z / 16.0D);

        this.activeProgram.setModelMatrix(matrixStack, x % 16.0D, y % 16.0D, z % 16.0D);

        int stride = this.activeProgram.vertexFormat.getStride();

        for (Map.Entry<BufferBlock, List<ShaderLCBRenderState>> entry : this.lists.entrySet()) {
            BufferBlock block = entry.getKey();
            block.bind(this.activeProgram.attributes);

            this.activeProgram.setModelOffset(block.getOrigin(), chunkX, chunkY, chunkZ);

            for (ShaderLCBRenderState state : entry.getValue()) {
                this.queueArray(state.getSegment(), stride);
            }

            this.drawArrays();

            block.unbind();
        }

        this.lists.clear();

        this.end(matrixStack);
    }

    private void queueArray(BufferSegment segment, int stride) {
        this.bufIndices.put(segment.getStart() / stride);
        this.bufLen.put(segment.getLength() / stride);
    }

    private void drawArrays() {
        this.bufIndices.flip();
        this.bufLen.flip();

        GL14.glMultiDrawArrays(GL11.GL_QUADS, this.bufIndices, this.bufLen);

        this.bufIndices.clear();
        this.bufLen.clear();
    }

    private void setupLists(Iterator<ShaderLCBRenderState> renders) {
        BufferBlock prevBlock = null;
        List<ShaderLCBRenderState> prevList = null;

        while (renders.hasNext()) {
            ShaderLCBRenderState state = renders.next();

            if (state == null) {
                continue;
            }

            BufferSegment slice = state.getSegment();
            BufferBlock block = slice.getBlock();

            List<ShaderLCBRenderState> list;

            if (prevBlock == block) {
                list = prevList;
            } else {
                list = this.lists.get(block);

                if (list == null) {
                    this.lists.put(block, list = new ArrayList<>());
                }

                prevList = list;
                prevBlock = block;
            }

            list.add(state);
        }
    }

    @Override
    public Class<ShaderLCBRenderState> getRenderStateType() {
        return ShaderLCBRenderState.class;
    }

    @Override
    protected ShaderLCBRenderState createRenderState(GlBuffer buffer, ChunkRender<ShaderLCBRenderState> render) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
        super.delete();

        this.bufferManager.delete();
        this.uploadBuffer.delete();
    }

    @Override
    public BlockPos getRenderOffset(ChunkSectionPos pos) {
        return this.bufferManager.getRenderOffset(pos);
    }

    public static boolean isSupported() {
        return GlVertexArray.isSupported() && GlBuffer.isBufferCopySupported();
    }
}
