package me.jellysquid.mods.thingl.texture;

import me.jellysquid.mods.thingl.GlObject;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import org.lwjgl.opengl.GL33C;

public class GlSampler extends GlObject {
    public GlSampler(RenderDeviceImpl device) {
        super(device);

        this.setHandle(GL33C.glGenSamplers());
    }

    public void bindTextureUnit(int unit) {
        GL33C.glBindSampler(unit, this.handle());
    }

    public void setParameter(int param, int value) {
        GL33C.glSamplerParameteri(this.handle(), param, value);
    }

    public void setParameter(int param, float value) {
        GL33C.glSamplerParameterf(this.handle(), param, value);
    }

    public void delete() {
        GL33C.glDeleteSamplers(this.handle());
        this.invalidateHandle();
    }

    public void unbindTextureUnit(int unit) {
        GL33C.glBindSampler(unit, 0);
    }
}
