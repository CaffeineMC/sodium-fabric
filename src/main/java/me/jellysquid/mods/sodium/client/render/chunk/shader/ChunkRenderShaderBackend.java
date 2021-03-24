package me.jellysquid.mods.sodium.client.render.chunk.shader;

import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.common.util.collections.IntPool;
import me.jellysquid.mods.sodium.common.util.collections.TrackedArray;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.EnumMap;

public abstract class ChunkRenderShaderBackend<T extends ChunkGraphicsState, P extends ChunkProgram>
        implements ChunkRenderBackend {
    private final EnumMap<ChunkFogMode, EnumMap<BlockRenderPass, P>> programs = new EnumMap<>(ChunkFogMode.class);

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected final TrackedArray<T> stateStorage;
    protected final IntPool stateIds;

    protected final ObjectArrayFIFOQueue<T> pendingUnloads = new ObjectArrayFIFOQueue<>();

    protected P activeProgram;

    public ChunkRenderShaderBackend(Class<T> graphicsType, ChunkVertexType vertexType) {
        this.stateStorage = new TrackedArray<>(graphicsType, 4096);
        this.stateIds = new IntPool();

        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();
    }

    @Override
    public final void createShaders() {
        for (ChunkFogMode fogMode : ChunkFogMode.values()) {
            this.programs.put(fogMode, createShadersForFogMode(fogMode));
        }
    }

    private final EnumMap<BlockRenderPass, P> createShadersForFogMode(ChunkFogMode mode) {
        EnumMap<BlockRenderPass, P> shaders = new EnumMap<>(BlockRenderPass.class);

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            shaders.put(pass, this.createShader(mode, pass, this.vertexFormat));
        }

        return shaders;
    }

    private P createShader(ChunkFogMode fogMode, BlockRenderPass pass, GlVertexFormat<ChunkMeshAttribute> format) {
        GlShader vertShader = this.createVertexShader(fogMode, pass);
        GlShader fragShader = this.createFragmentShader(fogMode, pass);

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader_for_" + pass.toString().toLowerCase()))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_Pos", format.getAttribute(ChunkMeshAttribute.POSITION))
                    .bindAttribute("a_Color", format.getAttribute(ChunkMeshAttribute.COLOR))
                    .bindAttribute("a_TexCoord", format.getAttribute(ChunkMeshAttribute.TEXTURE))
                    .bindAttribute("a_LightCoord", format.getAttribute(ChunkMeshAttribute.LIGHT))
                    .bindAttribute("mc_Entity", format.getAttribute(ChunkMeshAttribute.BLOCK_ID))
                    .bindAttribute("mc_midTexCoord", format.getAttribute(ChunkMeshAttribute.MID_TEX_COORD))
                    .bindAttribute("at_tangent", format.getAttribute(ChunkMeshAttribute.TANGENT))
                    .bindAttribute("a_Normal", format.getAttribute(ChunkMeshAttribute.NORMAL))
                    // TODO: This is hardcoded, we can't assume that index 8 will be okay
                    .bindAttribute("d_ModelOffset", 8)
                    .build((program, name) -> this.createShaderProgram(program, name, fogMode, pass));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    protected abstract GlShader createFragmentShader(ChunkFogMode fogMode, BlockRenderPass pass);

    protected abstract GlShader createVertexShader(ChunkFogMode fogMode, BlockRenderPass pass);

    protected abstract P createShaderProgram(Identifier name, int handle, ChunkFogMode fogMode, BlockRenderPass pass);

    @Override
    public void begin(MatrixStack matrixStack, BlockRenderPass pass) {
        this.unloadPending();

        this.activeProgram = this.programs.get(ChunkFogMode.getActiveMode()).get(pass);
        this.activeProgram.bind();
        this.activeProgram.setup(matrixStack, this.vertexType.getModelScale(), this.vertexType.getTextureScale());
    }

    @Override
    public void end(MatrixStack matrixStack) {
        this.activeProgram.unbind();
        this.activeProgram = null;
    }

    @Override
    public void delete() {
        for (EnumMap<BlockRenderPass, P> shaders: this.programs.values()) {
            for (P shader : shaders.values()) {
                shader.delete();
            }
        }

        this.unloadPending();
    }

    @Override
    public ChunkVertexType getVertexType() {
        return this.vertexType;
    }

    private void unloadPending() {
        while (!this.pendingUnloads.isEmpty()) {
            T state = this.pendingUnloads.dequeue();
            state.delete();

            this.stateIds.deallocateId(state.getId());
        }
    }

    @Override
    public void deleteGraphicsState(int id) {
        this.pendingUnloads.enqueue(this.stateStorage.remove(id));
    }
}
