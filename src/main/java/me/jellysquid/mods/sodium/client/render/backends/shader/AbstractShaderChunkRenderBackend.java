package me.jellysquid.mods.sodium.client.render.backends.shader;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.render.backends.AbstractChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.backends.shader.FogShaderComponent.FogMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.EnumMap;

public abstract class AbstractShaderChunkRenderBackend<T extends ChunkGraphicsState> extends AbstractChunkRenderBackend<T> {
    private final EnumMap<FogMode, ChunkProgram> shaders = new EnumMap<>(FogMode.class);

    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected ChunkProgram activeProgram;

    public AbstractShaderChunkRenderBackend(GlVertexFormat<ChunkMeshAttribute> format) {
        this.vertexFormat = format;

        this.shaders.put(FogMode.NONE, createShader(FogMode.NONE, format));
        this.shaders.put(FogMode.LINEAR, createShader(FogMode.LINEAR, format));
        this.shaders.put(FogMode.EXP2, createShader(FogMode.EXP2, format));
    }

    private static ChunkProgram createShader(FogMode fogMode, GlVertexFormat<ChunkMeshAttribute> format) {
        GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX, new Identifier("sodium", "chunk_gl20.v.glsl"), fogMode.getDefines());
        GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT, new Identifier("sodium", "chunk_gl20.f.glsl"), fogMode.getDefines());

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_Pos", format.getAttribute(ChunkMeshAttribute.POSITION))
                    .bindAttribute("a_Color", format.getAttribute(ChunkMeshAttribute.COLOR))
                    .bindAttribute("a_TexCoord", format.getAttribute(ChunkMeshAttribute.TEXTURE))
                    .bindAttribute("a_LightCoord", format.getAttribute(ChunkMeshAttribute.LIGHT))
                    .build((program, name) -> new ChunkProgram(program, name, fogMode.getFactory()));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    @Override
    public void begin(MatrixStack matrixStack) {
        super.begin(matrixStack);

        this.activeProgram = this.shaders.get(FogMode.getActiveMode());
        this.activeProgram.bind();
    }


    @Override
    public void end(MatrixStack matrixStack) {
        this.activeProgram.unbind();

        super.end(matrixStack);
    }

    @Override
    public void delete() {
        for (ChunkProgram shader : this.shaders.values()) {
            shader.delete();
        }
    }

    @Override
    public GlVertexFormat<ChunkMeshAttribute> getVertexFormat() {
        return this.vertexFormat;
    }
}
