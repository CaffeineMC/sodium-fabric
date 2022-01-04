package me.jellysquid.mods.sodium.opengl.shader;

import me.jellysquid.mods.sodium.opengl.ManagedObject;
import me.jellysquid.mods.sodium.opengl.shader.uniform.Uniform;
import me.jellysquid.mods.sodium.opengl.shader.uniform.UniformBlock;
import me.jellysquid.mods.sodium.opengl.shader.uniform.UniformFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL32C;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * An OpenGL shader program.
 */
public class ProgramImpl<T> extends ManagedObject implements ShaderBindingContext, Program<T> {
    private static final Logger LOGGER = LogManager.getLogger(ProgramImpl.class);

    private final T shaderInterface;

    public ProgramImpl(ShaderDescription description, Function<ShaderBindingContext, T> interfaceFactory) {
        var shaders = new ArrayList<Shader>();

        for (var entry : description.shaderSources.entrySet()) {
            shaders.add(new Shader(entry.getKey(), entry.getValue()));
        }

        int program = GL20C.glCreateProgram();

        for (var shader : shaders) {
            GL20C.glAttachShader(program, shader.handle());
        }

        for (var entry : description.attributeBindings.object2IntEntrySet()) {
            GL20C.glBindAttribLocation(program, entry.getIntValue(), entry.getKey());
        }

        for (var entry : description.attributeBindings.object2IntEntrySet()) {
            GL30C.glBindFragDataLocation(program, entry.getIntValue(), entry.getKey());
        }

        GL20C.glLinkProgram(program);

        for (var shader : shaders) {
            GL20C.glDetachShader(program, shader.handle());
            shader.delete();
        }

        printProgramLinkLog(program);
        verifyProgramLinked(program);

        this.setHandle(program);
        this.shaderInterface = interfaceFactory.apply(this);
    }

    private static void printProgramLinkLog(int program) {
        String log = GL20C.glGetProgramInfoLog(program);

        if (!log.isEmpty()) {
            LOGGER.warn("Program link log:" + log);
        }
    }

    private static void verifyProgramLinked(int program) {
        int result = GL20C.glGetProgrami(program, GL20C.GL_LINK_STATUS);

        if (result != GL20C.GL_TRUE) {
            throw new RuntimeException("Shader program linking failed, see log for details");
        }
    }

    @Override
    public T getInterface() {
        return this.shaderInterface;
    }

    @Override
    public void bindResources() {
        // NO-OP
    }

    @Override
    public void unbindResources() {
        // NO-OP
    }

    @Override
    public <U extends Uniform> U bindUniform(String name, UniformFactory<U> factory) {
        int handle = this.handle();
        int index = GL20C.glGetUniformLocation(handle, name);

        if (index < 0) {
            return null;
        }

        return factory.create(handle, index);
    }

    @Override
    public UniformBlock bindUniformBlock(String name, int bindingPoint) {
        int index = GL32C.glGetUniformBlockIndex(this.handle(), name);

        if (index < 0) {
            throw new NullPointerException("No uniform block exists with name: " + name);
        }

        GL32C.glUniformBlockBinding(this.handle(), index, bindingPoint);

        return new UniformBlock(bindingPoint);
    }

    /**
     * A compiled OpenGL shader object.
     */
    private static class Shader extends ManagedObject {
        private static final Logger LOGGER = LogManager.getLogger(Shader.class);

        private Shader(ShaderType type, String src) {
            int handle = GL20C.glCreateShader(type.id);
            ShaderWorkarounds.safeShaderSource(handle, src);
            GL20C.glCompileShader(handle);

            String log = GL20C.glGetShaderInfoLog(handle);

            if (!log.isEmpty()) {
                LOGGER.warn("Shader compilation log: " + log);
            }

            int result = GL20C.glGetShaderi(handle, GL20C.GL_COMPILE_STATUS);

            if (result != GL20C.GL_TRUE) {
                throw new RuntimeException("Shader compilation failed, see log for details");
            }

            this.setHandle(handle);
        }

        public void delete() {
            GL20C.glDeleteShader(this.handle());

            this.invalidateHandle();
        }
    }
}
