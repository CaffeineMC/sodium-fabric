package me.jellysquid.mods.thingl.texture;

import me.jellysquid.mods.thingl.GlObject;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import org.lwjgl.opengl.GL33C;

public class SamplerImpl extends GlObject implements Sampler {
    public SamplerImpl(RenderDeviceImpl device) {
        super(device);

        this.setHandle(GL33C.glGenSamplers());
    }

    @Override
    public void bindTextureUnit(int unit) {
        GL33C.glBindSampler(unit, this.handle());
    }

    @Override
    public void setParameter(int param, int value) {
        GL33C.glSamplerParameteri(this.handle(), param, value);
    }

    @Override
    public void setParameter(int param, float value) {
        GL33C.glSamplerParameterf(this.handle(), param, value);
    }

    public void delete() {
        GL33C.glDeleteSamplers(this.handle());
        this.invalidateHandle();
    }

    @Override
    public void unbindTextureUnit(int unit) {
        GL33C.glBindSampler(unit, 0);
    }
}
