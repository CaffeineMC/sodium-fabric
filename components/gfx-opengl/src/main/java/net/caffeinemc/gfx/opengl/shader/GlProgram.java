package net.caffeinemc.gfx.opengl.shader;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.caffeinemc.gfx.api.shader.*;
import net.caffeinemc.gfx.opengl.GlObject;
import net.caffeinemc.gfx.opengl.GlEnum;
import net.caffeinemc.gfx.opengl.shader.uniform.GlBufferBlock;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.function.Function;

/**
 * An OpenGL shader program.
 */
public class GlProgram<T> extends GlObject implements Program<T> {
    private final T shaderInterface;

    public GlProgram(ProgramDescription description, Function<ShaderBindingContext, T> interfaceFactory) {
        var shaders = new IntArrayList(description.shaders.size());

        for (var entry : description.shaders.entrySet()) {
            shaders.add(createShader(entry.getKey(), entry.getValue()));
        }

        int program = GL20C.glCreateProgram();

        for (var it = shaders.iterator(); it.hasNext(); ) {
            var shader = it.nextInt();
            GL20C.glAttachShader(program, shader);
        }

        GL20C.glLinkProgram(program);

        for (var it = shaders.iterator(); it.hasNext(); ) {
            var shader = it.nextInt();
            GL20C.glDetachShader(program, shader);
            GL20C.glDeleteShader(shader);
        }

        printProgramLinkLog(program);
        verifyProgramLinked(program);

        this.setHandle(program);

        try (var context = new BindingContext()) {
            this.shaderInterface = interfaceFactory.apply(context);
        }
    }

    private static int createShader(ShaderType type, ShaderDescription desc) {
        var source = desc.source();
        var specializationConstants = desc.specializationConstants();

        ByteBuffer sourceBuffer = MemoryUtil.memAlloc(source.length);
        sourceBuffer.put(source);
        sourceBuffer.flip();

        int shader = GL45C.glCreateShader(GlEnum.from(type));
        GL46C.glShaderBinary(new int[] { shader }, GL46C.GL_SHADER_BINARY_FORMAT_SPIR_V, sourceBuffer);

        try (var stack = MemoryStack.stackPush()) {
            var specializationIndices = stack.mallocInt(specializationConstants.size());
            var specializationValues = stack.mallocInt(specializationConstants.size());

            for (var constant : specializationConstants) {
                specializationIndices.put(constant.index());
                specializationValues.put(constant.value());
            }

            specializationIndices.flip();
            specializationValues.flip();

            GL46C.glSpecializeShader(shader, "main", specializationIndices, specializationValues);
        }

        MemoryUtil.memFree(sourceBuffer);

        String log = GL20C.glGetShaderInfoLog(shader);

        if (!log.isEmpty()) {
            System.err.println(log);
        }

        int result = GL20C.glGetShaderi(shader, GL20C.GL_COMPILE_STATUS);

        if (result != GL20C.GL_TRUE) {
            GL20C.glDeleteShader(shader);

            throw new RuntimeException("Shader compilation failed, see log for details");
        }

        return shader;
    }

    private static void printProgramLinkLog(int program) {
        String log = GL20C.glGetProgramInfoLog(program);

        if (!log.isEmpty()) {
            System.err.println(log);
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

    private class BindingContext implements ShaderBindingContext, AutoCloseable {
        private final int handle = GlProgram.this.handle();

        private boolean disposed;

        @Override
        public GlBufferBlock bindUniformBlock(int index) {
            this.checkDisposed();

            return new GlBufferBlock(GlProgram.this, index);
        }

        private void checkDisposed() {
            if (this.disposed) {
                throw new IllegalStateException("Binding context has been disposed");
            }
        }

        @Override
        public void close() {
            this.disposed = true;
        }
    }

    public static int getHandle(Program<?> program) {
        return ((GlProgram<?>) program).handle();
    }
}
