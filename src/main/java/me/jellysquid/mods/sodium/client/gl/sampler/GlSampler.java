package me.jellysquid.mods.sodium.client.gl.sampler;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import me.jellysquid.mods.sodium.client.gl.func.GlFunctions;

public class GlSampler extends GlObject {
    public GlSampler() {
        this.setHandle(GlFunctions.SAMPLER.glGenSamplers());
    }

    public void setParameter(int param, int value) {
        GlFunctions.SAMPLER.glSamplerParameteri(this.handle(), param, value);
    }

    public void bindToTextureUnit(int unit) {
        GlFunctions.SAMPLER.glBindSampler(unit, this.handle());
    }

    public void unbindFromTextureUnit(int unit) {
        GlFunctions.SAMPLER.glBindSampler(unit, 0);
    }

    public void delete() {
        GlFunctions.SAMPLER.glDeleteSamplers(this.handle());

        this.invalidateHandle();
    }
}
