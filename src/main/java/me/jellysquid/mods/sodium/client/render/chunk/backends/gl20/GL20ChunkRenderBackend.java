package me.jellysquid.mods.sodium.client.render.chunk.backends.gl20;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.oneshot.ChunkRenderBackendOneshot;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

/**
 * A simple chunk rendering backend which mirrors that of vanilla's own pretty closely.
 */
public class GL20ChunkRenderBackend extends ChunkRenderBackendOneshot<VBOGraphicsState> {
    public GL20ChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    public void begin(MatrixStack matrixStack) {
        super.begin(matrixStack);

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
    protected VBOGraphicsState createGraphicsState() {
        return new VBOGraphicsState();
    }

    public static boolean isSupported() {
        return true;
    }
}
