package me.jellysquid.mods.sodium.opengl.sampler;

import me.jellysquid.mods.sodium.opengl.ManagedObject;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL45C;

public class SamplerImpl extends ManagedObject implements Sampler {
    public SamplerImpl() {
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
