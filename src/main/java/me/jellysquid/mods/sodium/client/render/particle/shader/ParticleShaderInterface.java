package me.jellysquid.mods.sodium.client.render.particle.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat4v;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformMatrix4f;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderTextureSlot;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.client.util.TextureUtil;
import org.joml.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL32C;

public class ParticleShaderInterface {
    private final GlUniformInt uniformParticleTexture;
    private final GlUniformInt uniformLightTexture;
    private final GlUniformMatrix4f uniformModelViewMatrix;
    private final GlUniformMatrix4f uniformProjectionMatrix;
    private final GlUniformInt uniformBufferTexture;

    public ParticleShaderInterface(ShaderBindingContext context) {
        this.uniformParticleTexture = context.bindUniform("u_ParticleTex", GlUniformInt::new);
        this.uniformLightTexture = context.bindUniform("u_LightTex", GlUniformInt::new);
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", GlUniformMatrix4f::new);
        this.uniformBufferTexture = context.bindUniform("u_BufferTexture", GlUniformInt::new);
    }

    public void setProjectionMatrix(Matrix4fc matrix) {
        this.uniformProjectionMatrix.set(matrix);
    }

    public void setModelViewMatrix(Matrix4fc matrix) {
        this.uniformModelViewMatrix.set(matrix);
    }

    public void setupState() {
        // "BlockTexture" should represent the particle textures if bound correctly
        this.bindParticleTexture(ParticleShaderTextureSlot.TEXTURE, TextureUtil.getBlockTextureId());
        this.bindLightTexture(ParticleShaderTextureSlot.LIGHT, TextureUtil.getLightTextureId());
        this.bindBufferTexture(ParticleShaderTextureSlot.STORAGE, RenderSystem.getShaderTexture(3));
    }

    private void bindParticleTexture(ParticleShaderTextureSlot slot, int textureId) {
        GlStateManager._activeTexture(GL32C.GL_TEXTURE0 + slot.ordinal());
        GlStateManager._bindTexture(textureId);

        uniformParticleTexture.setInt(slot.ordinal());
    }

    private void bindLightTexture(ParticleShaderTextureSlot slot, int textureId) {
        GlStateManager._activeTexture(GL32C.GL_TEXTURE0 + slot.ordinal());
        GlStateManager._bindTexture(textureId);

        uniformLightTexture.setInt(slot.ordinal());
    }

    private void bindBufferTexture(ParticleShaderTextureSlot slot, int textureId) {
        GlStateManager._activeTexture(GL32C.GL_TEXTURE0 + slot.ordinal());
        GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, textureId);

        uniformBufferTexture.setInt(slot.ordinal());
    }

    private enum ParticleShaderTextureSlot {
        TEXTURE,
        LIGHT,
        STORAGE,
    }
}
