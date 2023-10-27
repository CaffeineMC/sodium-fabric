package me.jellysquid.mods.sodium.client.gl.shader.uniform;

import org.lwjgl.opengl.GL46C;

public class GlUniformFloat3v extends GlUniform<float[]> {
    public GlUniformFloat3v(int index) {
        super(index);
    }

    @Override
    public void set(float[] value) {
        if (value.length != 3) {
            throw new IllegalArgumentException("value.length != 3");
        }

        GL46C.glUniform3fv(this.index, value);
    }

    public void set(float x, float y, float z) {
        GL46C.glUniform3f(this.index, x, y, z);
    }
}
