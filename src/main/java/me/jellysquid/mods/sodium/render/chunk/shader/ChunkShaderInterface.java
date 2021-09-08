package me.jellysquid.mods.sodium.render.chunk.shader;

import me.jellysquid.mods.sodium.render.chunk.format.ModelVertexCompression;
import me.jellysquid.mods.thingl.buffer.MutableBuffer;
import me.jellysquid.mods.thingl.shader.ShaderBindingContext;
import me.jellysquid.mods.thingl.shader.uniform.GlUniformBlock;
import me.jellysquid.mods.thingl.shader.uniform.UniformFloat;
import me.jellysquid.mods.thingl.shader.uniform.UniformInt;
import me.jellysquid.mods.thingl.shader.uniform.UniformMatrix4F;
import org.joml.Matrix4f;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkShaderInterface {
    private final UniformFloat uniformModelScale;
    private final UniformFloat uniformModelOffset;
    private final UniformFloat uniformTextureScale;

    private final UniformMatrix4F uniformModelViewMatrix;
    private final UniformMatrix4F uniformProjectionMatrix;

    private final GlUniformBlock uniformBlockDrawParameters;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    private final DetailedShaderInterface detailBlock;

    private final UniformInt uniformBlockTex;
    private final UniformInt uniformLightTex;

    public ChunkShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", UniformMatrix4F::new);
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", UniformMatrix4F::new);

        this.uniformBlockTex = context.bindUniform("u_BlockTex", UniformInt::new);
        this.uniformLightTex = context.bindUniform("u_LightTex", UniformInt::new);

        this.uniformModelScale = context.bindUniform("u_ModelScale", UniformFloat::new);
        this.uniformModelOffset = context.bindUniform("u_ModelOffset", UniformFloat::new);
        this.uniformTextureScale = context.bindUniform("u_TextureScale", UniformFloat::new);

        this.uniformBlockDrawParameters = context.bindUniformBlock("ubo_DrawParameters", 0);

        this.detailBlock = options.pass().isDetail() ? new DetailedShaderInterface(context) : null;
        this.fogShader = options.fog().getFactory().apply(context);
    }

    public void setup() {
        this.uniformModelScale.setFloat(ModelVertexCompression.getModelScale());
        this.uniformModelOffset.setFloat(ModelVertexCompression.getModelOffset());
        this.uniformTextureScale.setFloat(ModelVertexCompression.getTextureScale());
        
        this.fogShader.setup();

        this.uniformBlockTex.setInt(ChunkShaderTextureUnit.BLOCK_TEXTURE.id());
        this.uniformLightTex.setInt(ChunkShaderTextureUnit.LIGHT_TEXTURE.id());

        if (this.detailBlock != null) {
            this.detailBlock.setup();
        }
    }

    public void setProjectionMatrix(Matrix4f matrix) {
        this.uniformProjectionMatrix.set(matrix);
    }

    public void setModelViewMatrix(Matrix4f matrix) {
        this.uniformModelViewMatrix.set(matrix);
    }

    public void setDrawUniforms(MutableBuffer buffer) {
        this.uniformBlockDrawParameters.bindBuffer(buffer);
    }

    public void setDetailParameters(float detailDistance) {
        if (this.detailBlock != null) {
            this.detailBlock.setDetailDistance(detailDistance);
        }
    }

    public static class DetailedShaderInterface {
        private final UniformFloat uniformDetailNearPlane;
        private final UniformFloat uniformDetailFarPlane;
        private final UniformInt uniformStippleTex;

        public DetailedShaderInterface(ShaderBindingContext context) {
            this.uniformDetailNearPlane = context.bindUniform("u_DetailNearPlane", UniformFloat::new);
            this.uniformDetailFarPlane = context.bindUniform("u_DetailFarPlane", UniformFloat::new);

            this.uniformStippleTex = context.bindUniform("u_StippleTex", UniformInt::new);
        }

        public void setDetailDistance(float distance) {
            this.uniformDetailNearPlane.setFloat(distance - 18.0f);
            this.uniformDetailFarPlane.setFloat(distance - 9.0f);
        }

        public void setup() {
            this.uniformStippleTex.setInt(ChunkShaderTextureUnit.STIPPLE_TEXTURE.id());
        }
    }
}
