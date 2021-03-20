package me.jellysquid.mods.sodium.client.render.chunk.backends.gl30;

import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import me.jellysquid.mods.sodium.client.gl.util.MemoryTracker;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.oneshot.ChunkRenderBackendOneshot;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Shader-based render backend for chunks which uses VAOs to avoid the overhead in setting up vertex attribute pointers
 * before every draw call. This approach has significantly less CPU overhead as we only need to cross the native code
 * barrier once in order to setup all the necessary vertex attribute states and buffer bindings. Additionally, it might
 * allow the driver to skip validation logic that would otherwise be performed.
 */
public class GL30ChunkRenderBackend extends ChunkRenderBackendOneshot<VAOGraphicsState> {
    public GL30ChunkRenderBackend(ChunkVertexType vertexType) {
        super(VAOGraphicsState.class, vertexType);
    }

    @Override
    public void end(MatrixStack matrixStack) {
        GlFunctions.VERTEX_ARRAY.glBindVertexArray(0);

        super.end(matrixStack);
    }

    @Override
    protected VAOGraphicsState createGraphicsState(MemoryTracker memoryTracker, ChunkRenderContainer container, int id) {
        return new VAOGraphicsState(memoryTracker, container, id);
    }

    public static boolean isSupported(boolean disableBlacklist) {
        return GlFunctions.isVertexArraySupported();
    }

    @Override
    public String getRendererName() {
        return "Oneshot (GL 3.0)";
    }
}
