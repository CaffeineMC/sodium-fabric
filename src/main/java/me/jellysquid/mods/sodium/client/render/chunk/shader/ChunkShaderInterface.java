package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformBlock;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformMatrix4f;
import me.jellysquid.mods.sodium.client.gl.texture.GlTexture;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.opengl.GL32C;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkShaderInterface {
    private final GlUniformFloat uniformModelScale;
    private final GlUniformFloat uniformModelOffset;
    private final GlUniformFloat uniformTextureScale;

    private final GlUniformInt uniformBlockTex;
    private final GlUniformInt uniformLightTex;

    private final GlUniformMatrix4f uniformModelViewMatrix;
    private final GlUniformMatrix4f uniformProjectionMatrix;

    private final GlUniformBlock uniformBlockDrawParameters;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    private final DetailedShaderInterface detailBlock;

    public ChunkShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", GlUniformMatrix4f::new);

        this.uniformBlockTex = context.bindUniform("u_BlockTex", GlUniformInt::new);
        this.uniformLightTex = context.bindUniform("u_LightTex", GlUniformInt::new);

        this.uniformModelScale = context.bindUniform("u_ModelScale", GlUniformFloat::new);
        this.uniformModelOffset = context.bindUniform("u_ModelOffset", GlUniformFloat::new);
        this.uniformTextureScale = context.bindUniform("u_TextureScale", GlUniformFloat::new);

        this.uniformBlockDrawParameters = context.bindUniformBlock("ubo_DrawParameters", 0);

        this.detailBlock = options.pass().isDetail() ? new DetailedShaderInterface(context) : null;

        this.fogShader = options.fog().getFactory().apply(context);
    }

    public void setup(ChunkVertexType vertexType) {
        RenderSystem.activeTexture(GL32C.GL_TEXTURE0);
        RenderSystem.bindTexture(RenderSystem.getShaderTexture(0));
        this.uniformBlockTex.setInt(0);

        RenderSystem.activeTexture(GL32C.GL_TEXTURE2);
        RenderSystem.bindTexture(RenderSystem.getShaderTexture(2));
        this.uniformLightTex.setInt(2);

        this.uniformModelScale.setFloat(vertexType.getModelScale());
        this.uniformModelOffset.setFloat(vertexType.getModelOffset());
        this.uniformTextureScale.setFloat(vertexType.getTextureScale());
        
        this.fogShader.setup();
    }

    public void setProjectionMatrix(Matrix4f matrix) {
        this.uniformProjectionMatrix.set(matrix);
    }

    public void setModelViewMatrix(Matrix4f matrix) {
        this.uniformModelViewMatrix.set(matrix);
    }

    public void setDrawUniforms(GlMutableBuffer buffer) {
        this.uniformBlockDrawParameters.bindBuffer(buffer);
    }

    public void setDetailParameters(GlTexture stippleTexture, float detailDistance) {
        if (this.detailBlock != null) {
            this.detailBlock.setStippleTexture(stippleTexture);
            this.detailBlock.setDetailDistance(detailDistance);
        }
    }

    public static class DetailedShaderInterface {
        private final GlUniformInt uniformStippleTex;
        private final GlUniformFloat uniformDetailNearPlane;
        private final GlUniformFloat uniformDetailFarPlane;

        public DetailedShaderInterface(ShaderBindingContext context) {
            this.uniformStippleTex = context.bindUniform("u_StippleTex", GlUniformInt::new);
            this.uniformDetailNearPlane = context.bindUniform("u_DetailNearPlane", GlUniformFloat::new);
            this.uniformDetailFarPlane = context.bindUniform("u_DetailFarPlane", GlUniformFloat::new);
        }

        public void setStippleTexture(GlTexture texture) {
            RenderSystem.activeTexture(GL32C.GL_TEXTURE9);
            RenderSystem.bindTexture(texture.handle());

            this.uniformStippleTex.setInt(9);
        }

        public void setDetailDistance(float distance) {
            this.uniformDetailNearPlane.setFloat(distance - 24.0f);
            this.uniformDetailFarPlane.setFloat(distance - 12.0f);
        }
    }
}
