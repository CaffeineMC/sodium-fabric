package me.jellysquid.mods.sodium.client.render.particle.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat4v;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformMatrix4f;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderTextureSlot;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.client.util.TextureUtil;
import org.joml.*;
import org.lwjgl.opengl.GL32C;

public class ParticleShaderInterface {
    private final GlUniformInt uniformParticleTexture;
    private final GlUniformInt uniformLightTexture;
    private final GlUniformMatrix4f uniformModelViewMatrix;
    private final GlUniformMatrix4f uniformProjectionMatrix;
    private final GlUniformFloat4v uniformCameraRotation;

    public ParticleShaderInterface(ShaderBindingContext context) {
        this.uniformParticleTexture = context.bindUniform("u_ParticleTex", GlUniformInt::new);
        this.uniformLightTexture = context.bindUniform("u_LightTex", GlUniformInt::new);
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", GlUniformMatrix4f::new);
        this.uniformCameraRotation = context.bindUniform("u_CameraRotation", GlUniformFloat4v::new);
    }

    public void setProjectionMatrix(Matrix4fc matrix) {
        this.uniformProjectionMatrix.set(matrix);
    }

    public void setModelViewMatrix(Matrix4fc matrix) {
        this.uniformModelViewMatrix.set(matrix);
    }
    public void setCameraRotation(Quaternionfc quaternion) {
        this.uniformCameraRotation.set(new float[] {
                quaternion.x(),
                quaternion.y(),
                quaternion.z(),
                quaternion.w(),
        });
    }

    public void setupState() {
        // "BlockTexture" should represent the particle textures if bound correctly
        this.bindParticleTexture(ParticleShaderTextureSlot.TEXTURE, TextureUtil.getBlockTextureId());
        this.bindLightTexture(ParticleShaderTextureSlot.LIGHT, TextureUtil.getLightTextureId());
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

    private enum ParticleShaderTextureSlot {
        TEXTURE,
        LIGHT,
    }
}
