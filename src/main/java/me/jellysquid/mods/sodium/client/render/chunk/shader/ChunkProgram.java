package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.texture.ChunkProgramTextureComponent;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.List;

/**
 * A forward-rendering shader program for chunks.
 */
public abstract class ChunkProgram extends GlProgram {
    // The model size of a chunk (16^3)
    protected static final float MODEL_SIZE = 32.0f;

    // Uniform variable binding indexes
    private final int uModelViewProjectionMatrix;
    private final int uModelScale;

    public final ChunkProgramTextureComponent texture;
    public final ChunkShaderFogComponent fog;

    private final List<ShaderComponent> components;

    protected ChunkProgram(Identifier name, int handle, ChunkProgramComponentBuilder components) {
        super(name, handle);

        this.uModelViewProjectionMatrix = this.getUniformLocation("u_ModelViewProjectionMatrix");
        this.uModelScale = this.getUniformLocation("u_ModelScale");

        this.texture = components.texture.create(this);
        this.fog = components.fog.create(this);

        this.components = ImmutableList.of(this.texture, this.fog);
    }

    @Override
    public void bind(MatrixStack matrixStack) {
        super.bind(matrixStack);

        for (ShaderComponent component : this.components) {
             component.bind();
        }

        GL20.glUniform3f(this.uModelScale, MODEL_SIZE, MODEL_SIZE, MODEL_SIZE);

        MatrixStack.Entry matrices = matrixStack.peek();

        // Since vanilla doesn't expose the projection matrix anywhere, we need to grab it from the OpenGL state
        // This isn't super fast, but should be sufficient enough to remain compatible with any state modifying code
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer bufProjection = stack.mallocFloat(16);
            FloatBuffer bufModelView = stack.mallocFloat(16);
            FloatBuffer bufModelViewProjection = stack.mallocFloat(16);

            GL15.glGetFloatv(GL15.GL_PROJECTION_MATRIX, bufProjection);
            matrices.getModel().writeToBuffer(bufModelView);

            GL11.glPushMatrix();
            GL11.glLoadMatrixf(bufProjection);
            GL11.glMultMatrixf(bufModelView);
            GL15.glGetFloatv(GL15.GL_MODELVIEW_MATRIX, bufModelViewProjection);
            GL11.glPopMatrix();

            GL20.glUniformMatrix4fv(this.uModelViewProjectionMatrix, false, bufModelViewProjection);
        }
    }

    @Override
    public void unbind() {
        super.unbind();

        for (ShaderComponent component : this.components) {
            component.unbind();
        }
    }

    @Override
    public void delete() {
        super.delete();

        for (ShaderComponent component : this.components) {
            component.delete();
        }
    }
}
