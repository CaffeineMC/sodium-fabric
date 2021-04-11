package me.jellysquid.mods.sodium.client.render.chunk.backends.gl20;

import me.jellysquid.mods.sodium.client.gl.util.MemoryTracker;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.oneshot.ChunkRenderBackendOneshot;
import net.minecraft.client.util.math.MatrixStack;

import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

/**
 * A simple chunk rendering backend which mirrors that of vanilla's own pretty closely.
 */
public class GL20ChunkRenderBackend extends ChunkRenderBackendOneshot<VBOGraphicsState> {
    public GL20ChunkRenderBackend(ChunkVertexType format) {
        super(format);
    }

    @Override
    public void begin(MatrixStack matrixStack, BlockRenderPass pass) {
        super.begin(matrixStack, pass);

        this.vertexFormat.enableVertexAttributes();
    }

    @Override
    public void end(MatrixStack matrixStack) {
        this.vertexFormat.disableVertexAttributes();
        GL20.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        super.end(matrixStack);
    }

    @Override
    public Class<VBOGraphicsState> getGraphicsStateType() {
        return VBOGraphicsState.class;
    }

    @Override
    protected VBOGraphicsState createGraphicsState(MemoryTracker memoryTracker, ChunkRenderContainer<VBOGraphicsState> container) {
        return new VBOGraphicsState(memoryTracker, container);
    }

    public static boolean isSupported(boolean disableBlacklist) {
        return true;
    }

    @Override
    public String getRendererName() {
        return "Oneshot (GL 2.0)";
    }
}
