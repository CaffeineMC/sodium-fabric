package me.jellysquid.mods.sodium.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.texture.GlSampler;
import org.lwjgl.opengl.GL33C;

public class TextureUtil {
    public static GlSampler createLightTextureSampler() {
        var sampler = new GlSampler();
        sampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR);
        sampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR);
        sampler.setParameter(GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
        sampler.setParameter(GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);

        return sampler;
    }

    public static GlSampler createBlockTextureSampler(boolean mipped) {
        var sampler = new GlSampler();
        sampler.setParameter(GL33C.GL_TEXTURE_MIN_FILTER, mipped ? GL33C.GL_NEAREST_MIPMAP_LINEAR : GL33C.GL_NEAREST);
        sampler.setParameter(GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);
        sampler.setParameter(GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
        sampler.setParameter(GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);

        return sampler;
    }

    /**
     * NOTE: Must be called while a RenderLayer is active.
     */
    public static int getLightTextureId() {
        return RenderSystem.getShaderTexture(2);
    }

    /**
     * NOTE: Must be called while a RenderLayer is active.
     */
    public static int getBlockTextureId() {
        return RenderSystem.getShaderTexture(0);
    }
}
