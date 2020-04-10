package me.jellysquid.mods.sodium.client.gl.shader;

import me.jellysquid.mods.sodium.client.gl.GlHandle;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL21;

import java.util.function.IntFunction;

public abstract class GlShaderProgram extends GlHandle {
    protected GlShaderProgram(int program) {
        this.setHandle(program);
    }

    public static Builder builder() {
        return new Builder();
    }

    public void bind() {
        GL21.glUseProgram(this.handle());
    }

    public void unbind() {
        GL21.glUseProgram(0);
    }

    public int getUniformLocation(String name) {
        int index = GL21.glGetUniformLocation(this.handle(), name);

        if (index < 0) {
            throw new NullPointerException("No uniform exists with name: " + name);
        }

        return index;
    }

    public void delete() {
        GL21.glDeleteProgram(this.handle());

        this.invalidateHandle();
    }

    public static class Builder {
        private final int program;

        public Builder() {
            this.program = GL21.glCreateProgram();
        }

        public Builder attach(GlShader shader) {
            GL21.glAttachShader(this.program, shader.handle());

            return this;
        }

        public <P extends GlShaderProgram> P link(IntFunction<P> factory) {
            GL21.glLinkProgram(this.program);

            int result = GL21.glGetProgrami(this.program, GL21.GL_LINK_STATUS);

            if (result != GL11.GL_TRUE) {
                throw new RuntimeException("Shader link error: " + GL21.glGetProgramInfoLog(this.program));
            }

            return factory.apply(this.program);
        }
    }
}
