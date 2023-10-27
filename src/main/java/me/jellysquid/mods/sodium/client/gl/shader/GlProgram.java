package me.jellysquid.mods.sodium.client.gl.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.client.gl.GlObject;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniform;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformBlock;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL46C;

import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * An OpenGL shader program.
 */
public class GlProgram<T> extends GlObject implements ShaderBindingContext {
    private static final Logger LOGGER = LogManager.getLogger(GlProgram.class);

    private final T shaderInterface;

    protected GlProgram(int program, Function<ShaderBindingContext, T> interfaceFactory) {
        this.setHandle(program);
        this.shaderInterface = interfaceFactory.apply(this);
    }

    public T getInterface() {
        return this.shaderInterface;
    }

    public static Builder builder(Identifier identifier) {
        return new Builder(identifier);
    }

    public void bind() {
        GL46C.glUseProgram(this.handle());
    }

    public void unbind() {
        GL46C.glUseProgram(0);
    }

    public void delete() {
        GL46C.glDeleteProgram(this.handle());

        this.invalidateHandle();
    }

    @Override
    public <U extends GlUniform<?>> U bindUniform(String name, IntFunction<U> factory) {
        int index = GL46C.glGetUniformLocation(this.handle(), name);

        if (index < 0) {
            throw new NullPointerException("No uniform exists with name: " + name);
        }

        return factory.apply(index);
    }

    @Override
    public GlUniformBlock bindUniformBlock(String name, int bindingPoint) {
        int index = GL46C.glGetUniformBlockIndex(this.handle(), name);

        if (index < 0) {
            throw new NullPointerException("No uniform block exists with name: " + name);
        }

        GL46C.glUniformBlockBinding(this.handle(), index, bindingPoint);

        return new GlUniformBlock(bindingPoint);
    }

    public static class Builder {
        private final Identifier name;
        private final int program;

        public Builder(Identifier name) {
            this.name = name;
            this.program = GL46C.glCreateProgram();
        }

        public Builder attachShader(GlShader shader) {
            GL46C.glAttachShader(this.program, shader.handle());

            return this;
        }

        /**
         * Links the attached shaders to this program and returns a user-defined container which wraps the shader
         * program. This container can, for example, provide methods for updating the specific uniforms of that shader
         * set.
         *
         * @param factory The factory which will create the shader program's interface
         * @param <U> The interface type for the shader program
         * @return An instantiated shader container as provided by the factory
         */
        public <U> GlProgram<U> link(Function<ShaderBindingContext, U> factory) {
            GL46C.glLinkProgram(this.program);

            String log = GL46C.glGetProgramInfoLog(this.program);

            if (!log.isEmpty()) {
                LOGGER.warn("Program link log for " + this.name + ": " + log);
            }

            int result = GlStateManager.glGetProgrami(this.program, GL46C.GL_LINK_STATUS);

            if (result != GL46C.GL_TRUE) {
                throw new RuntimeException("Shader program linking failed, see log for details");
            }

            return new GlProgram<>(this.program, factory);
        }

        public Builder bindAttribute(String name, int index) {
            GL46C.glBindAttribLocation(this.program, index, name);

            return this;
        }

        public Builder bindFragmentData(String name, int index) {
            GL46C.glBindFragDataLocation(this.program, index, name);

            return this;
        }
    }
}
