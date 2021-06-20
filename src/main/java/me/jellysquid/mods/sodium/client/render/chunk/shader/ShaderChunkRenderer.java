package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.gl.compat.LegacyFogHelper;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.EnumMap;

public abstract class ShaderChunkRenderer implements ChunkRenderer {
    private final EnumMap<ChunkFogMode, ChunkProgram> programs = new EnumMap<>(ChunkFogMode.class);

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected ChunkProgram activeProgram;

    public ShaderChunkRenderer(RenderDevice device, ChunkVertexType vertexType) {
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();

        this.programs.put(ChunkFogMode.NONE, this.createShader(device, ChunkFogMode.NONE));
        this.programs.put(ChunkFogMode.SMOOTH, this.createShader(device, ChunkFogMode.SMOOTH));
    }

    private ChunkProgram createShader(RenderDevice device, ChunkFogMode fogMode) {
        GlShader vertShader = ShaderLoader.loadShader(device, ShaderType.VERTEX,
                new Identifier("sodium", "chunk_gl32.v.glsl"), fogMode.getDefines());

        GlShader fragShader = ShaderLoader.loadShader(device, ShaderType.FRAGMENT,
                new Identifier("sodium", "chunk_gl32.f.glsl"), fogMode.getDefines());

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_Pos", ChunkShaderBindingPoints.POSITION)
                    .bindAttribute("a_Color", ChunkShaderBindingPoints.COLOR)
                    .bindAttribute("a_TexCoord", ChunkShaderBindingPoints.TEX_COORD)
                    .bindAttribute("a_LightCoord", ChunkShaderBindingPoints.LIGHT_COORD)
                    .bindAttribute("d_ModelOffset", ChunkShaderBindingPoints.MODEL_OFFSET)
                    .bindFragmentData("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
                    .build((program, name) -> new ChunkProgram(device, program, name, fogMode.getFactory()));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    protected void begin(MatrixStack matrixStack) {
        this.activeProgram = this.programs.get(LegacyFogHelper.getFogMode());
        this.activeProgram.bind();
        this.activeProgram.setup(matrixStack, this.vertexType.getModelScale(), this.vertexType.getTextureScale());
    }

    protected void end() {
        this.activeProgram.unbind();
        this.activeProgram = null;
    }

    protected abstract GlTessellation createRegionTessellation(CommandList commandList, GlBufferArena vertices, GlBufferArena indices);

    @Override
    public void delete() {
        for (ChunkProgram shader : this.programs.values()) {
            shader.delete();
        }
    }

    @Override
    public ChunkVertexType getVertexType() {
        return this.vertexType;
    }
}
