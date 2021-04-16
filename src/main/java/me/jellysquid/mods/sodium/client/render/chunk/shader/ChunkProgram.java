package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.GameRendererContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import java.util.function.Function;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkProgram extends GlProgram {
    // Uniform variable binding indexes
    private final int uModelViewProjectionMatrix;
    private final int uModelScale;
    private final int uTextureScale;
    private final int uBlockTex;
    private final int uLightTex;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    protected ChunkProgram(RenderDevice owner, Identifier name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction) {
        super(owner, name, handle);

        this.uModelViewProjectionMatrix = this.getUniformLocation("u_ModelViewProjectionMatrix");

        this.uBlockTex = this.getUniformLocation("u_BlockTex");
        this.uLightTex = this.getUniformLocation("u_LightTex");
        this.uModelScale = this.getUniformLocation("u_ModelScale");
        this.uTextureScale = this.getUniformLocation("u_TextureScale");

        this.fogShader = fogShaderFunction.apply(this);
    }

    public void setup(MatrixStack matrixStack, float modelScale, float textureScale) {
        GL20C.glUniform1i(this.uBlockTex, 0);
        GL20C.glUniform1i(this.uLightTex, 2);

        GL20C.glUniform3f(this.uModelScale, modelScale, modelScale, modelScale);
        GL20C.glUniform2f(this.uTextureScale, textureScale, textureScale);

        this.fogShader.setup();

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            GL20C.glUniformMatrix4fv(this.uModelViewProjectionMatrix, false,
                    GameRendererContext.getModelViewProjectionMatrix(matrixStack.peek(), memoryStack));
        }
    }
}
