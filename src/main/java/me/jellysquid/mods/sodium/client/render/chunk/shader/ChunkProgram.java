package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import net.coderbot.iris.texunits.TextureUnit;
import net.coderbot.iris.uniforms.SamplerUniforms;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.function.Function;

/**
 * A forward-rendering shader program for chunks.
 */
public abstract class ChunkProgram extends GlProgram {
    // Uniform variable binding indexes
    private final int uModelViewProjectionMatrix;
    private final int uModelScale;
    private final int uTextureScale;
    private final int uBlockTex;
    private final int uLightTex;
    private final int uNoiseTex;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    protected ChunkProgram(Identifier name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction) {
        super(name, handle);

        this.uModelViewProjectionMatrix = this.getUniformLocation("u_ModelViewProjectionMatrix");

        this.uBlockTex = this.getUniformLocation("u_BlockTex");
        this.uLightTex = this.getUniformLocation("u_LightTex");
        this.uNoiseTex = this.getUniformLocation("noisetex");
        this.uModelScale = this.getUniformLocation("u_ModelScale");
        this.uTextureScale = this.getUniformLocation("u_TextureScale");

        this.fogShader = fogShaderFunction.apply(this);
    }

    public void setup(MatrixStack matrixStack, float modelScale, float textureScale) {
        GL20.glUniform1i(this.uBlockTex, TextureUnit.TERRAIN.getSamplerId());
        GL20.glUniform1i(this.uLightTex, TextureUnit.LIGHTMAP.getSamplerId());
        GL20.glUniform1i(this.uNoiseTex, SamplerUniforms.NOISE_TEX);

        GL20.glUniform3f(this.uModelScale, modelScale, modelScale, modelScale);
        GL20.glUniform2f(this.uTextureScale, textureScale, textureScale);

        // TODO: set up noise texture!!!

        this.fogShader.setup();

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
}
