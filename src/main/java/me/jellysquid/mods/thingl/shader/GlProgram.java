package me.jellysquid.mods.thingl.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.thingl.GlObject;
import me.jellysquid.mods.thingl.device.RenderDevice;
import me.jellysquid.mods.thingl.device.RenderDeviceImpl;
import me.jellysquid.mods.thingl.shader.uniform.GlUniform;
import me.jellysquid.mods.thingl.shader.uniform.GlUniformBlock;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL32C;

import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * An OpenGL shader program.
 */
public class GlProgram<T> extends GlObject implements ShaderBindingContext {
    private static final Logger LOGGER = LogManager.getLogger(GlProgram.class);

    private final T shaderInterface;

    public GlProgram(RenderDeviceImpl device, GlShader[] shaders, Function<ShaderBindingContext, T> interfaceFactory) {
        super(device);

        int program = GL20C.glCreateProgram();

        this.setHandle(program);

        for (GlShader shader : shaders) {
            GL20C.glAttachShader(program, shader.handle());
        }

        GL20C.glLinkProgram(program);

        String log = GL20C.glGetProgramInfoLog(program);

        if (!log.isEmpty()) {
            LOGGER.warn("Program link log: " + log);
        }

        int result = GlStateManager.glGetProgrami(program, GL20C.GL_LINK_STATUS);

        if (result != GL20C.GL_TRUE) {
            throw new RuntimeException("Shader program linking failed, see log for details");
        }

        this.shaderInterface = interfaceFactory.apply(this);
    }

    public T getInterface() {
        return this.shaderInterface;
    }

    public void bind() {
        var tracker = this.device.getStateTracker();
        var handle = this.handle();

        if (tracker.makeProgramActive(handle)) {
            GL20C.glUseProgram(handle);
        }
    }

    public void unbind() {
        GL20C.glUseProgram(0);
    }

    public void delete() {
        GL20C.glDeleteProgram(this.handle());

        this.invalidateHandle();
    }

    @Override
    public <U extends GlUniform<?>> U bindUniform(String name, IntFunction<U> factory) {
        int index = GL20C.glGetUniformLocation(this.handle(), name);

        if (index < 0) {
            throw new NullPointerException("No uniform exists with name: " + name);
        }

        return factory.apply(index);
    }

    @Override
    public GlUniformBlock bindUniformBlock(String name, int bindingPoint) {
        int index = GL32C.glGetUniformBlockIndex(this.handle(), name);

        if (index < 0) {
            throw new NullPointerException("No uniform block exists with name: " + name);
        }

        GL32C.glUniformBlockBinding(this.handle(), index, bindingPoint);

        return new GlUniformBlock(bindingPoint);
    }
}
