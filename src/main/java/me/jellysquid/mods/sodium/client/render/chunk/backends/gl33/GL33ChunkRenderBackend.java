package me.jellysquid.mods.sodium.client.render.chunk.backends.gl33;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.gl.util.MemoryTracker;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.oneshot.ChunkProgramOneshot;
import me.jellysquid.mods.sodium.client.render.chunk.oneshot.ChunkRenderBackendOneshot;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.chunk.passes.impl.MultiTextureRenderPipeline;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgramComponentBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.shader.texture.ChunkProgramMultiTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Shader-based render backend for chunks which uses VAOs to avoid the overhead in setting up vertex attribute pointers
 * before every draw call. This approach has significantly less CPU overhead as we only need to cross the native code
 * barrier once in order to setup all the necessary vertex attribute states and buffer bindings. Additionally, it might
 * allow the driver to skip validation logic that would otherwise be performed.
 */
public class GL33ChunkRenderBackend extends ChunkRenderBackendOneshot<GL33GraphicsState> {
    private final BlockRenderPassManager renderPassManager = MultiTextureRenderPipeline.create();

    public GL33ChunkRenderBackend(GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        super(format);
    }

    @Override
    protected void modifyProgram(GlProgram.Builder builder, ChunkProgramComponentBuilder components,
                                 GlVertexFormat<SodiumVertexFormats.ChunkMeshAttribute> format) {
        components.texture = ChunkProgramMultiTexture::new;
    }

    @Override
    protected ChunkProgramOneshot createShaderProgram(Identifier name, int handle, ChunkProgramComponentBuilder components) {
        return new ChunkProgramOneshot(name, handle, components);
    }

    @Override
    protected void addShaderConstants(ShaderConstants.Builder builder) {
        builder.define("USE_MULTITEX");
    }

    @Override
    public void endRender(MatrixStack matrixStack) {
        GlFunctions.VERTEX_ARRAY.glBindVertexArray(0);

        super.endRender(matrixStack);
    }

    @Override
    public Class<GL33GraphicsState> getGraphicsStateType() {
        return GL33GraphicsState.class;
    }

    @Override
    public BlockRenderPassManager getRenderPassManager() {
        return this.renderPassManager;
    }

    @Override
    protected GL33GraphicsState createGraphicsState(MemoryTracker memoryTracker, ChunkRenderContainer<GL33GraphicsState> container) {
        return new GL33GraphicsState(memoryTracker, container);
    }

    public static boolean isSupported(boolean disableBlacklist) {
        return GlFunctions.isVertexArraySupported() && GlFunctions.isSamplerSupported();
    }

    @Override
    public String getRendererName() {
        return "Oneshot (GL 3.3)";
    }
}
