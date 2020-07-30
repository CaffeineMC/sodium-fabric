package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.SodiumHooks;
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

    private final EnumMap<ChunkFogMode, P> programsWithCulling = new EnumMap<>(ChunkFogMode.class);

    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected P activeProgram;

    public ChunkRenderShaderBackend(GlVertexFormat<ChunkMeshAttribute> format) {
        this.vertexFormat = format;
    }

    @Override
    public final void createShaders() {
        this.programs.put(ChunkFogMode.NONE, this.createShader(ChunkFogMode.NONE, this.vertexFormat, false));
        this.programs.put(ChunkFogMode.LINEAR, this.createShader(ChunkFogMode.LINEAR, this.vertexFormat, false));
        this.programs.put(ChunkFogMode.EXP2, this.createShader(ChunkFogMode.EXP2, this.vertexFormat, false));

        this.programsWithCulling.put(ChunkFogMode.NONE, this.createShader(ChunkFogMode.NONE, this.vertexFormat, true));
        this.programsWithCulling.put(ChunkFogMode.LINEAR, this.createShader(ChunkFogMode.LINEAR, this.vertexFormat, true));
        this.programsWithCulling.put(ChunkFogMode.EXP2, this.createShader(ChunkFogMode.EXP2, this.vertexFormat, true));

    }

    private P createShader(ChunkFogMode fogMode, GlVertexFormat<ChunkMeshAttribute> format, boolean useCulling) {
        GlShader vertShader = this.createVertexShader(fogMode, useCulling);
        GlShader fragShader = this.createFragmentShader(fogMode, useCulling);

        ChunkProgramComponentBuilder components = new ChunkProgramComponentBuilder();
        components.fog = fogMode.getFactory();

        if (useCulling) {
            components.cull = ChunkShaderCullingComponent::new;
        }

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

    private GlShader createVertexShader(ChunkFogMode fogMode, boolean useCulling) {
        return ShaderLoader.loadShader(ShaderType.VERTEX, new Identifier("sodium", "chunk_glsl110.v.glsl"),
                this.createShaderConstants(fogMode, useCulling));
    }

    private GlShader createFragmentShader(ChunkFogMode fogMode, boolean useCulling) {
        return ShaderLoader.loadShader(ShaderType.FRAGMENT, new Identifier("sodium", "chunk_glsl110.f.glsl"),
                this.createShaderConstants(fogMode, useCulling));
    }

    private ShaderConstants createShaderConstants(ChunkFogMode fogMode, boolean useCulling) {
        ShaderConstants.Builder builder = ShaderConstants.builder();
        fogMode.addConstants(builder);

        if (useCulling) {
            builder.define("USE_CULLING");
        }

        this.addShaderConstants(builder);

        return builder.build();
    }

    protected abstract void addShaderConstants(ShaderConstants.Builder builder);

    protected abstract P createShaderProgram(Identifier name, int handle, ChunkProgramComponentBuilder components);

    protected void beginRender(MatrixStack matrixStack, BlockRenderPass pass) {
        if (SodiumHooks.shouldEnableCulling.getAsBoolean()) {
            this.activeProgram = this.programsWithCulling.get(ChunkFogMode.getActiveMode());
        } else {
            this.activeProgram = this.programs.get(ChunkFogMode.getActiveMode());
        }
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
