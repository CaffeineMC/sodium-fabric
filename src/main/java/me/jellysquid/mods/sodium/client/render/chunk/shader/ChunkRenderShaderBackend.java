package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.SodiumHooks;
import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
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
        this.programs.put(ChunkFogMode.NONE, this.createShader(ChunkFogMode.NONE, false, this.vertexFormat));
        this.programs.put(ChunkFogMode.LINEAR, this.createShader(ChunkFogMode.LINEAR, false, this.vertexFormat));
        this.programs.put(ChunkFogMode.EXP2, this.createShader(ChunkFogMode.EXP2, false, this.vertexFormat));

        this.programsWithCulling.put(ChunkFogMode.NONE, this.createShader(ChunkFogMode.NONE, true, this.vertexFormat));
        this.programsWithCulling.put(ChunkFogMode.LINEAR, this.createShader(ChunkFogMode.LINEAR, true, this.vertexFormat));
        this.programsWithCulling.put(ChunkFogMode.EXP2, this.createShader(ChunkFogMode.EXP2, true, this.vertexFormat));
    }

    private P createShader(ChunkFogMode fogMode, boolean useCulling, GlVertexFormat<ChunkMeshAttribute> format) {
        GlShader vertShader = this.createVertexShader(fogMode, useCulling);
        GlShader fragShader = this.createFragmentShader(fogMode, useCulling);

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_Pos", format.getAttribute(ChunkMeshAttribute.POSITION))
                    .bindAttribute("a_Color", format.getAttribute(ChunkMeshAttribute.COLOR))
                    .bindAttribute("a_TexCoord", format.getAttribute(ChunkMeshAttribute.TEXTURE))
                    .bindAttribute("a_LightCoord", format.getAttribute(ChunkMeshAttribute.LIGHT))
                    .build((program, name) -> this.createShaderProgram(program, name, fogMode, useCulling));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    protected abstract GlShader createFragmentShader(ChunkFogMode fogMode, boolean useCulling);

    protected abstract GlShader createVertexShader(ChunkFogMode fogMode, boolean useCulling);

    protected abstract P createShaderProgram(Identifier name, int handle, ChunkFogMode fogMode, boolean useCulling);

    @Override
    public void begin(MatrixStack matrixStack) {
        EnumMap<ChunkFogMode, P> programSet = SodiumHooks.shouldEnableCulling.getAsBoolean() ?
                this.programsWithCulling : this.programs;
        this.activeProgram = programSet.get(ChunkFogMode.getActiveMode());
        this.activeProgram.bind();
        this.activeProgram.setup(matrixStack);
    }

    @Override
    public void end(MatrixStack matrixStack) {
        this.activeProgram.unbind();
    }

    @Override
    public void delete() {
        for (P shader : this.programs.values()) {
            shader.delete();
        }
        for (P shader : this.programsWithCulling.values()) {
            shader.delete();
        }
    }

    @Override
    public GlVertexFormat<ChunkMeshAttribute> getVertexFormat() {
        return this.vertexFormat;
    }
}
