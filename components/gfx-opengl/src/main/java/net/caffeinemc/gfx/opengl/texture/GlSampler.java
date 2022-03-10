package net.caffeinemc.gfx.opengl.texture;

import net.caffeinemc.gfx.opengl.GlObject;
import net.caffeinemc.gfx.api.texture.Sampler;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL45C;

public class GlSampler extends GlObject implements Sampler {
    public GlSampler() {
        this.setHandle(GL33C.glGenSamplers());
    }

    @Override
    public void setParameter(int parameter, int value) {
        GL45C.glSamplerParameteri(this.handle(), parameter, value);
    }

    @Override
    public void setParameter(int parameter, float value) {
        GL45C.glSamplerParameterf(this.handle(), parameter, value);
    }
}
