package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.ARBDrawBuffersBlend;
import org.lwjgl.opengl.GL20;

import java.util.EnumMap;

public abstract class ChunkRenderShaderBackend<T extends ChunkGraphicsState, P extends ChunkProgram>
        implements ChunkRenderBackend<T> {
    private final EnumMap<ChunkFogMode, P> programs = new EnumMap<>(ChunkFogMode.class);
    private final EnumMap<ChunkFogMode, P> translucencyPrograms = new EnumMap<>(ChunkFogMode.class);

    protected final ChunkVertexType vertexType;
    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;

    protected P activeProgram;

    public ChunkRenderShaderBackend(ChunkVertexType vertexType) {
        this.vertexType = vertexType;
        this.vertexFormat = vertexType.getCustomVertexFormat();
    }

    @Override
    public final void createShaders() {
        this.createShader(ChunkFogMode.NONE, this.vertexFormat, false);
        this.createShader(ChunkFogMode.LINEAR, this.vertexFormat, false);
        this.createShader(ChunkFogMode.EXP2, this.vertexFormat, false);

        this.createShader(ChunkFogMode.NONE, this.vertexFormat, true);
        this.createShader(ChunkFogMode.LINEAR, this.vertexFormat, true);
        this.createShader(ChunkFogMode.EXP2, this.vertexFormat, true);
    }

    private void createShader(ChunkFogMode fogMode, GlVertexFormat<ChunkMeshAttribute> format, boolean translucent) {
        GlShader vertShader = this.createVertexShader(fogMode);
        GlShader fragShader;
        if (translucent) {
            fragShader = this.createTranslucencyFragmentShader(fogMode);
        } else {
            fragShader = this.createFragmentShader(fogMode);
        }

        try {
            P prog = GlProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attachShader(vertShader)
                    .attachShader(fragShader)
                    .bindAttribute("a_Pos", format.getAttribute(ChunkMeshAttribute.POSITION))
                    .bindAttribute("a_Color", format.getAttribute(ChunkMeshAttribute.COLOR))
                    .bindAttribute("a_TexCoord", format.getAttribute(ChunkMeshAttribute.TEXTURE))
                    .bindAttribute("a_LightCoord", format.getAttribute(ChunkMeshAttribute.LIGHT))
                    .build((program, name) -> this.createShaderProgram(program, name, fogMode));
            if (translucent) {
                this.translucencyPrograms.put(fogMode, prog);
            } else {
                this.programs.put(fogMode, prog);
            }
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    protected abstract GlShader createFragmentShader(ChunkFogMode fogMode);
    protected abstract GlShader createTranslucencyFragmentShader(ChunkFogMode fogMode);
    protected abstract GlShader createVertexShader(ChunkFogMode fogMode);
    protected abstract P createShaderProgram(Identifier name, int handle, ChunkFogMode fogMode);

    @Override
    public void begin(MatrixStack matrixStack, boolean translucent) {
        if (translucent) {
            this.activeProgram = this.translucencyPrograms.get(ChunkFogMode.getActiveMode());
            GlStateManager.blendFunc(GL20.GL_ONE, GL20.GL_ONE);
            ARBDrawBuffersBlend.glBlendFunciARB(1, GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_COLOR);
        } else {
            this.activeProgram = this.programs.get(ChunkFogMode.getActiveMode());
        }
        this.activeProgram.bind();
        this.activeProgram.setup(matrixStack);
    }

    @Override
    public void end(MatrixStack matrixStack) {
        this.activeProgram.unbind();
        this.activeProgram = null;
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public void delete() {
        for (P shader : this.programs.values()) {
            shader.delete();
        }
    }

    @Override
    public ChunkVertexType getVertexType() {
        return this.vertexType;
    }
}
