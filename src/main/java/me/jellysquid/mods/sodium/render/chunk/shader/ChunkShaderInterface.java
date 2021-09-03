package me.jellysquid.mods.sodium.render.chunk.shader;

import me.jellysquid.mods.sodium.render.chunk.format.ModelVertexCompression;
import me.jellysquid.mods.thingl.buffer.GlMutableBuffer;
import me.jellysquid.mods.thingl.shader.ShaderBindingContext;
import me.jellysquid.mods.thingl.shader.uniform.GlUniformBlock;
import me.jellysquid.mods.thingl.shader.uniform.GlUniformFloat;
import me.jellysquid.mods.thingl.shader.uniform.GlUniformInt;
import me.jellysquid.mods.thingl.shader.uniform.GlUniformMatrix4f;
import me.jellysquid.mods.sodium.model.vertex.type.ChunkVertexType;
import org.joml.Matrix4f;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkShaderInterface {
    private final GlUniformFloat uniformModelScale;
    private final GlUniformFloat uniformModelOffset;
    private final GlUniformFloat uniformTextureScale;

    private final GlUniformMatrix4f uniformModelViewMatrix;
    private final GlUniformMatrix4f uniformProjectionMatrix;

    private final GlUniformBlock uniformBlockDrawParameters;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    private final DetailedShaderInterface detailBlock;

    private final GlUniformInt uniformBlockTex;
    private final GlUniformInt uniformLightTex;

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
        this.uniformModelScale.setFloat(ModelVertexCompression.getModelScale());
        this.uniformModelOffset.setFloat(ModelVertexCompression.getModelOffset());
        this.uniformTextureScale.setFloat(ModelVertexCompression.getTextureScale());
        
        this.fogShader.setup();

        this.uniformBlockTex.setInt(ChunkShaderTextureUnit.BLOCK_TEXTURE.id());
        this.uniformLightTex.setInt(ChunkShaderTextureUnit.LIGHT_TEXTURE.id());

        if (this.detailBlock != null) {
            this.detailBlock.setup(vertexType);
        }
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

    public void setDetailParameters(float detailDistance) {
        if (this.detailBlock != null) {
            this.detailBlock.setDetailDistance(detailDistance);
        }
    }

    public static class DetailedShaderInterface {
        private final GlUniformFloat uniformDetailNearPlane;
        private final GlUniformFloat uniformDetailFarPlane;
        private final GlUniformInt uniformStippleTex;

        public DetailedShaderInterface(ShaderBindingContext context) {
            this.uniformDetailNearPlane = context.bindUniform("u_DetailNearPlane", GlUniformFloat::new);
            this.uniformDetailFarPlane = context.bindUniform("u_DetailFarPlane", GlUniformFloat::new);

            this.uniformStippleTex = context.bindUniform("u_StippleTex", GlUniformInt::new);
        }

        public void setDetailDistance(float distance) {
            this.uniformDetailNearPlane.setFloat(distance - 18.0f);
            this.uniformDetailFarPlane.setFloat(distance - 9.0f);
        }

        public void setup(ChunkVertexType vertexType) {
            this.uniformStippleTex.setInt(ChunkShaderTextureUnit.STIPPLE_TEXTURE.id());
        }
    }
}
