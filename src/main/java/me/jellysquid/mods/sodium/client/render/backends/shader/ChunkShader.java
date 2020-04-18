package me.jellysquid.mods.sodium.client.render.backends.shader;

import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.shader.GlShaderProgram;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.function.Function;

public class ChunkShader extends GlShaderProgram {
    private final int uModelViewMatrix;
    private final int uProjectionMatrix;
    private final int uModelOffset;
    private final int uBlockTex;
    private final int uLightTex;

    private final FogShaderComponent fogShader;
    private final FloatBuffer uModelOffsetBuffer;

    public final GlAttributeBinding[] attributes;

    public ChunkShader(Identifier name, int handle, Function<ChunkShader, FogShaderComponent> fogShaderFunction) {
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

        // TODO: Create this from VertexFormat
        this.attributes = new GlAttributeBinding[] {
                new GlAttributeBinding(aPos, 3, GL11.GL_FLOAT, false, 32, 0),
                new GlAttributeBinding(aColor, 4, GL11.GL_UNSIGNED_BYTE, true, 32, 12),
                new GlAttributeBinding(aTexCoord, 2, GL11.GL_FLOAT, false, 32, 16),
                new GlAttributeBinding(aLightCoord, 2, GL11.GL_SHORT, false, 32, 24)
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

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer bufProjection = stack.mallocFloat(16);
            GL15.glGetFloatv(GL15.GL_PROJECTION_MATRIX, bufProjection);

            GL20.glUniformMatrix4fv(this.uProjectionMatrix, false, bufProjection);
        }
    }

    public void setModelMatrix(MatrixStack.Entry matrices) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer bufModelView = stack.mallocFloat(16);
            matrices.getModel().writeToBuffer(bufModelView);

            GL20.glUniformMatrix4fv(this.uModelViewMatrix, false, bufModelView);
        }
    }

    public void setModelOffset(Vector3d translation, double x, double y, double z) {
        FloatBuffer buf = this.uModelOffsetBuffer;
        buf.put(0, (float) (translation.x - x));
        buf.put(1, (float) (translation.y - y));
        buf.put(2, (float) (translation.z - z));

        GL20.glUniform3fv(this.uModelOffset, buf);
    }
}
