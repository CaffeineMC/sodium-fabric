package me.jellysquid.mods.sodium.client.render.backends.shader;

import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.shader.GlShaderProgram;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class ChunkShader extends GlShaderProgram {
    private final int uModelView;
    private final int uProjection;
    private final int uBlockTex;
    private final int uLightTex;

    public final GlAttributeBinding[] attributes;

    public ChunkShader(Identifier name, int handle) {
        super(name, handle);

        this.uModelView = this.getUniformLocation("u_ModelView");
        this.uProjection = this.getUniformLocation("u_Projection");

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
    }

    @Override
    public void bind() {
        super.bind();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buf = stack.mallocFloat(16);

            GL15.glGetFloatv(GL15.GL_PROJECTION_MATRIX, buf);
            GL20.glUniformMatrix4fv(this.uProjection, false, buf);
        }

        GL20.glUniform1i(this.uBlockTex, 0);
        GL20.glUniform1i(this.uLightTex, 2);
    }

    public void uploadModelMatrix(FloatBuffer buffer) {
        GL20.glUniformMatrix4fv(this.uModelView, false, buffer);
    }
}
