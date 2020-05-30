package me.jellysquid.mods.sodium.client.render.backends.shader;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlImmutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.GlShader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderLoader;
import me.jellysquid.mods.sodium.client.gl.shader.ShaderType;
import me.jellysquid.mods.sodium.client.render.backends.AbstractChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.backends.shader.FogShaderComponent.FogMode;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMesh;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderData;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL15;

import java.util.EnumMap;
import java.util.Iterator;

public abstract class AbstractShaderChunkRenderBackend<T extends ChunkRenderState> extends AbstractChunkRenderBackend<T> {
    private final EnumMap<FogMode, ChunkProgram> shaders = new EnumMap<>(FogMode.class);

    protected final GlVertexFormat<ChunkMeshAttribute> vertexFormat;
    protected final boolean useImmutableStorage;

    protected ChunkProgram activeProgram;

    public AbstractShaderChunkRenderBackend(GlVertexFormat<ChunkMeshAttribute> format) {
        this.vertexFormat = format;
        this.useImmutableStorage = GlImmutableBuffer.isSupported() && SodiumClientMod.options().performance.useImmutableStorage;

        this.shaders.put(FogMode.NONE, createShader(format, FogMode.NONE));
        this.shaders.put(FogMode.LINEAR, createShader(format, FogMode.LINEAR));
        this.shaders.put(FogMode.EXP2, createShader(format, FogMode.EXP2));
    }

    private static ChunkProgram createShader(GlVertexFormat<ChunkMeshAttribute> format, FogMode fogMode) {
        GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX, new Identifier("sodium:chunk_gl20.v.glsl"), fogMode.getDefines());
        GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT, new Identifier("sodium:chunk_gl20.f.glsl"), fogMode.getDefines());

        try {
            return GlProgram.builder(new Identifier("sodium", "chunk_shader"))
                    .attach(vertShader)
                    .attach(fragShader)
                    .link((program, name) -> new ChunkProgram(program, name, format, fogMode.getFactory()));
        } finally {
            vertShader.delete();
            fragShader.delete();
        }
    }

    @Override
    public void upload(Iterator<ChunkBuildResult<T>> queue) {
        GlBuffer lastBuffer = null;

        while (queue.hasNext()) {
            ChunkBuildResult<T> result = queue.next();

            ChunkRenderContainer<T> render = result.render;
            ChunkRenderData data = result.data;

            render.resetRenderStates();
            render.setData(data);

            for (ChunkMesh mesh : data.getMeshes()) {
                GlBuffer buffer = this.createBuffer();
                buffer.bind(GL15.GL_ARRAY_BUFFER);
                buffer.upload(GL15.GL_ARRAY_BUFFER, mesh.takePendingUpload());

                lastBuffer = buffer;

                render.setRenderState(mesh.getRenderPass(), this.createRenderState(buffer, render));
            }
        }

        if (lastBuffer != null) {
            lastBuffer.unbind(GL15.GL_ARRAY_BUFFER);
        }
    }

    private GlBuffer createBuffer() {
        return this.useImmutableStorage ? new GlImmutableBuffer(0) : new GlMutableBuffer(GL15.GL_STATIC_DRAW);
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

    protected abstract T createRenderState(GlBuffer buffer, ChunkRenderContainer<T> render);

    @Override
    public GlVertexFormat<ChunkMeshAttribute> getVertexFormat() {
        return this.vertexFormat;
    }
}
