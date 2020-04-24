package me.jellysquid.mods.sodium.client.render.backends.shader.lcb;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.BufferUploadData;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.memory.BufferBlock;
import me.jellysquid.mods.sodium.client.gl.memory.BufferSegment;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMesh;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRender;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.opengl.GL15;

import java.util.Iterator;

public class ShaderLCBChunkRenderBackend extends AbstractShaderChunkRenderBackend<ShaderLCBRenderState> {
    private final ChunkRegionManager bufferManager;
    private final GlMutableBuffer uploadBuffer;

    private final ObjectList<ChunkRegion> pendingBatches = new ObjectArrayList<>();

    public ShaderLCBChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);

        this.bufferManager = new ChunkRegionManager();
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

                ChunkRegion region = this.bufferManager.createRegion(pos);
                BufferBlock block = region.getBuffer();

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

                render.setRenderState(mesh.getRenderPass(), new ShaderLCBRenderState(region, segment, this.vertexFormat));
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

        this.setupBatches(renders);
        this.begin(matrixStack);

        int chunkX = (int) (x / 16.0D);
        int chunkY = (int) (y / 16.0D);
        int chunkZ = (int) (z / 16.0D);

        this.activeProgram.setModelMatrix(matrixStack, x % 16.0D, y % 16.0D, z % 16.0D);

        GlVertexArray prevArray = null;

        for (ChunkRegion region : this.pendingBatches) {
            this.activeProgram.setModelOffset(region.getOrigin(), chunkX, chunkY, chunkZ);

            prevArray = region.drawBatch(this.activeProgram.attributes);
        }

        if (prevArray != null) {
            prevArray.unbind();
        }

        this.pendingBatches.clear();

        this.end(matrixStack);
    }

    private void setupBatches(Iterator<ShaderLCBRenderState> renders) {
        while (renders.hasNext()) {
            ShaderLCBRenderState state = renders.next();

            if (state != null) {
                ChunkRegion region = state.getRegion();

                if (region.isBatchEmpty()) {
                    this.pendingBatches.add(region);
                }

                region.addToBatch(state);
            }
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
