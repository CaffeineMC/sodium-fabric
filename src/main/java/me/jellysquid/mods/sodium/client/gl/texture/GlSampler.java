package me.jellysquid.mods.sodium.client.gl.texture;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import org.lwjgl.opengl.GL33C;

public class GlSampler extends GlObject {

    public GlSampler() {
        this.setHandle(GL33C.glGenSamplers());
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
}
