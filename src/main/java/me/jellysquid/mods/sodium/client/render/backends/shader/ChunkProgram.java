package me.jellysquid.mods.sodium.client.render.backends.shader;

import me.jellysquid.mods.sodium.client.gl.SodiumVertexFormats.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkSectionPos;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.function.Function;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkProgram extends GlProgram {
    // The model size of a chunk (16^3)
    private static final float MODEL_SIZE = 16.0f;

    // Uniform variable binding indexes
    private final int uModelViewMatrix;
    private final int uProjectionMatrix;
    private final int uModelOffset;
    private final int uBlockTex;
    private final int uLightTex;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final FogShaderComponent fogShader;

    // Scratch buffer
    private final FloatBuffer uModelOffsetBuffer;

    public final GlVertexAttributeBinding[] attributes;

    public ChunkProgram(Identifier name, int handle, GlVertexFormat<ChunkMeshAttribute> format, Function<ChunkProgram, FogShaderComponent> fogShaderFunction) {
        super(name, handle);

        this.uModelViewMatrix = this.getUniformLocation("u_ModelViewMatrix");
        this.uProjectionMatrix = this.getUniformLocation("u_ProjectionMatrix");
        this.uModelOffset = this.getUniformLocation("u_ModelOffset");

        this.uBlockTex = this.getUniformLocation("u_BlockTex");
        this.uLightTex = this.getUniformLocation("u_LightTex");

        int aPos = this.getAttributeLocation("a_Pos");
        int aColor = this.getAttributeLocation("a_Color");
        int aTexCoord = this.getAttributeLocation("a_TexCoord");
        int aLightCoord = this.getAttributeLocation("a_LightCoord");

        this.attributes = new GlVertexAttributeBinding[] {
                new GlVertexAttributeBinding(aPos, format, ChunkMeshAttribute.POSITION),
                new GlVertexAttributeBinding(aColor, format, ChunkMeshAttribute.COLOR),
                new GlVertexAttributeBinding(aTexCoord, format, ChunkMeshAttribute.TEXTURE),
                new GlVertexAttributeBinding(aLightCoord, format, ChunkMeshAttribute.LIGHT)
        };

        this.uModelOffsetBuffer = MemoryUtil.memAllocFloat(3);
        this.fogShader = fogShaderFunction.apply(this);
    }

    @Override
    public void bind() {
        super.bind();

        GL20.glUniform1i(this.uBlockTex, 0);
        GL20.glUniform1i(this.uLightTex, 2);

        this.fogShader.setup();

        // Since vanilla doesn't expose the projection matrix anywhere, we need to grab it from the OpenGL state
        // This isn't super fast, but should be sufficient enough to remain compatible with any state modifying code
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer bufProjection = stack.mallocFloat(16);
            GL15.glGetFloatv(GL15.GL_PROJECTION_MATRIX, bufProjection);

            GL20.glUniformMatrix4fv(this.uProjectionMatrix, false, bufProjection);
        }
    }

    /**
     * Sets up the model-view matrix used for rendering a set of chunks. This should be called once before rendering any
     * chunks or after the matrix stack changes.
     * @param matrixStack The current stack which contains the existing model-view matrix on top
     * @param x The camera's x-position
     * @param y The camera's y-position
     * @param z The camera's z-position
     */
    public void setupModelViewMatrix(MatrixStack matrixStack, double x, double y, double z) {
        matrixStack.push();
        matrixStack.translate(-x, -y, -z);

        MatrixStack.Entry entry = matrixStack.peek();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer bufModelView = stack.mallocFloat(16);
            entry.getModel().writeToBuffer(bufModelView);

            GL20.glUniformMatrix4fv(this.uModelViewMatrix, false, bufModelView);
        }

        matrixStack.pop();
    }

    /**
     * Sets up the translation for rendering a chunk. In order to avoid floating point inaccuracies caused by
     * very large numbers in transformation matrices, this method will only translate the chunk based on its local
     * distance in view space.
     * @param pos The origin of the chunk being rendered
     * @param offsetX The x-offset of the chunk the player is currently in
     * @param offsetY The y-offset of the chunk the player is currently in
     * @param offsetZ The z-offset of the chunk the player is currently in
     */
    public void setupChunk(ChunkSectionPos pos, int offsetX, int offsetY, int offsetZ) {
        FloatBuffer buf = this.uModelOffsetBuffer;
        buf.put(0, (pos.getX() - offsetX) * MODEL_SIZE);
        buf.put(1, (pos.getY() - offsetY) * MODEL_SIZE);
        buf.put(2, (pos.getZ() - offsetZ) * MODEL_SIZE);

        GL20.glUniform3fv(this.uModelOffset, buf);
    }
}
