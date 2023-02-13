package me.jellysquid.mods.sodium.client.render.chunk.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.*;
import me.jellysquid.mods.sodium.client.gl.texture.GlSampler;
import me.jellysquid.mods.sodium.client.util.TextureUtil;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL33C;

import java.util.EnumMap;
import java.util.Map;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkShaderInterface {
    private final Map<ChunkShaderTextureSlot, GlUniformInt> uniformTextures;

    private final GlUniformMatrix4f uniformModelViewMatrix;
    private final GlUniformMatrix4f uniformProjectionMatrix;
    private final GlUniformFloat3v uniformRegionOffset;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    public ChunkShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", GlUniformMatrix4f::new);
        this.uniformRegionOffset = context.bindUniform("u_RegionOffset", GlUniformFloat3v::new);

        this.uniformTextures = new EnumMap<>(ChunkShaderTextureSlot.class);
        this.uniformTextures.put(ChunkShaderTextureSlot.BLOCK, context.bindUniform("u_BlockTex", GlUniformInt::new));
        this.uniformTextures.put(ChunkShaderTextureSlot.BLOCK_MIPPED, context.bindUniform("u_BlockMippedTex", GlUniformInt::new));
        this.uniformTextures.put(ChunkShaderTextureSlot.LIGHT, context.bindUniform("u_LightTex", GlUniformInt::new));

        this.fogShader = options.fog().getFactory().apply(context);
    }

    @Deprecated // the shader interface should not modify pipeline state
    public void startDrawing(EnumMap<ChunkShaderTextureSlot, GlSampler> samplers) {
        for (ChunkShaderTextureSlot slot : ChunkShaderTextureSlot.VALUES) {
            this.bindTexture(slot, getTextureId(slot), samplers.get(slot));
        }

        this.fogShader.setup();
    }

    @Deprecated // the shader interface should not modify pipeline state
    public void endDrawing() {
        for (ChunkShaderTextureSlot slot : ChunkShaderTextureSlot.VALUES) {
            this.unbindTexture(slot);
        }
    }

    @Deprecated(forRemoval = true) // should be handled properly in GFX instead.
    private void bindTexture(ChunkShaderTextureSlot slot, int textureId, GlSampler sampler) {
        GlStateManager._activeTexture(GL32C.GL_TEXTURE0 + slot.ordinal());
        GlStateManager._bindTexture(textureId);

        GL33C.glBindSampler(slot.ordinal(), sampler.handle());

        var uniform = this.uniformTextures.get(slot);
        uniform.setInt(slot.ordinal());
    }

    @Deprecated(forRemoval = true) // should be handled properly in GFX instead.
    private void unbindTexture(ChunkShaderTextureSlot slot) {
        GL33C.glBindSampler(slot.ordinal(), 0);
    }

    public void setProjectionMatrix(Matrix4f matrix) {
        this.uniformProjectionMatrix.set(matrix);
    }

    public void setModelViewMatrix(Matrix4f matrix) {
        this.uniformModelViewMatrix.set(matrix);
    }

    public void setRegionOffset(float x, float y, float z) {
        this.uniformRegionOffset.set(x, y, z);
    }

    private static int getTextureId(ChunkShaderTextureSlot slot) {
        return switch (slot) {
            case BLOCK, BLOCK_MIPPED -> TextureUtil.getBlockTextureId();
            case LIGHT -> TextureUtil.getLightTextureId();
        };
    }
}
