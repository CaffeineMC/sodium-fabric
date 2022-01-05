package me.jellysquid.mods.sodium.opengl.pipeline;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.opengl.sampler.Sampler;
import me.jellysquid.mods.sodium.opengl.sampler.SamplerImpl;
import org.lwjgl.opengl.GL45C;

import java.util.BitSet;

public class Blaze3DPipelineState implements PipelineState {
    private final BitSet changedTextures = new BitSet(GL45C.glGetInteger(GL45C.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS));

    @Override
    public void bindTexture(int unit, int texture, Sampler sampler) {
        this.changedTextures.set(unit);

        GL45C.glBindTextureUnit(unit, texture);

        // TODO: require a valid sampler object
        if (sampler instanceof SamplerImpl samplerImpl) {
            GL45C.glBindSampler(unit, samplerImpl.handle());
        } else {
            GL45C.glBindSampler(unit, 0);
        }
    }

    @Override
    public void restoreState() {
        for (int unit = this.changedTextures.nextSetBit(0); unit != -1; unit = this.changedTextures.nextSetBit(unit + 1)) {
            GL45C.glBindTextureUnit(unit, GlStateManager.TEXTURES[unit].boundTexture);
            GL45C.glBindSampler(unit, 0);
        }

        this.changedTextures.clear();
    }
}
