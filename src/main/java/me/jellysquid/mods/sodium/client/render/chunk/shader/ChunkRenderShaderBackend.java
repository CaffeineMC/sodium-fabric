package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.EnumMap;

public abstract class ChunkRenderShaderBackend<T extends ChunkGraphicsState, P extends ChunkProgram>
        implements ChunkRenderBackend<T> {
    private final EnumMap<ChunkFogMode, P> programs = new EnumMap<>(ChunkFogMode.class);

    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected P activeProgram;

    public ChunkRenderShaderBackend(GlVertexFormat<ChunkMeshAttribute> format) {
        this.vertexFormat = format;
    }

    @Override
    public final void createShaders() {
        this.programs.put(ChunkFogMode.NONE, this.createShader(ChunkFogMode.NONE, this.vertexFormat));
        this.programs.put(ChunkFogMode.LINEAR, this.createShader(ChunkFogMode.LINEAR, this.vertexFormat));
        this.programs.put(ChunkFogMode.EXP2, this.createShader(ChunkFogMode.EXP2, this.vertexFormat));
    }

    private P createShader(ChunkFogMode fogMode, GlVertexFormat<ChunkMeshAttribute> format) {
        GlShader vertShader = this.createVertexShader(fogMode);
        GlShader fragShader = this.createFragmentShader(fogMode);

        ChunkProgramComponentBuilder components = new ChunkProgramComponentBuilder();
        components.fog = fogMode.getFactory();

        try {
            GlProgram.Builder builder = GlProgram.builder(new Identifier("sodium", "chunk_shader"));
            builder.attachShader(vertShader);
            builder.attachShader(fragShader);

            builder.bindAttribute("a_Pos", format.getAttribute(ChunkMeshAttribute.POSITION));
            builder.bindAttribute("a_Color", format.getAttribute(ChunkMeshAttribute.COLOR));
            builder.bindAttribute("a_TexCoord", format.getAttribute(ChunkMeshAttribute.TEXTURE));
            builder.bindAttribute("a_LightCoord", format.getAttribute(ChunkMeshAttribute.LIGHT));

            this.modifyProgram(builder, components, format);

            return builder.build((program, name) -> this.createShaderProgram(program, name, components));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    protected abstract void modifyProgram(GlProgram.Builder builder, ChunkProgramComponentBuilder components,
                                          GlVertexFormat<ChunkMeshAttribute> format);

    private GlShader createVertexShader(ChunkFogMode fogMode) {
        return ShaderLoader.loadShader(ShaderType.VERTEX, new Identifier("sodium", "chunk_glsl110.v.glsl"),
                this.createShaderConstants(fogMode));
    }

    private GlShader createFragmentShader(ChunkFogMode fogMode) {
        return ShaderLoader.loadShader(ShaderType.FRAGMENT, new Identifier("sodium", "chunk_glsl110.f.glsl"),
                this.createShaderConstants(fogMode));
    }

    private ShaderConstants createShaderConstants(ChunkFogMode fogMode) {
        ShaderConstants.Builder builder = ShaderConstants.builder();
        fogMode.addConstants(builder);

        this.addShaderConstants(builder);

        return builder.build();
    }

    protected abstract void addShaderConstants(ShaderConstants.Builder builder);

    protected abstract P createShaderProgram(Identifier name, int handle, ChunkProgramComponentBuilder components);

    protected void beginRender(MatrixStack matrixStack, BlockRenderPass pass) {
        this.activeProgram = this.programs.get(ChunkFogMode.getActiveMode());
        this.activeProgram.bind(matrixStack);
    }

    protected void endRender(MatrixStack matrixStack) {
        this.activeProgram.unbind();
    }

    @Override
    public void delete() {
        for (P shader : this.programs.values()) {
            shader.delete();
        }
    }

    @Override
    public GlVertexFormat<ChunkMeshAttribute> getVertexFormat() {
        return this.vertexFormat;
    }
}
