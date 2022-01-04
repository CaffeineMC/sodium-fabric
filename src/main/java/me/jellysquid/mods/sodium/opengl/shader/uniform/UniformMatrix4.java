package me.jellysquid.mods.sodium.opengl.shader.uniform;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class UniformMatrix4 extends Uniform {
    private UniformMatrix4(int program, int index) {
        super(program, index);
    }

    public static UniformFactory<UniformMatrix4> of() {
        return UniformMatrix4::new;
    }

    public void set(Matrix4f matrix4) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buf = stack.callocFloat(16);
            matrix4.get(buf);

            GL45C.glProgramUniformMatrix4fv(this.program, this.index, false, buf);
        }
    }

    @Deprecated // Vanilla type
    public void set(net.minecraft.util.math.Matrix4f matrix4) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buf = stack.callocFloat(16);
            matrix4.writeColumnMajor(buf);

            GL45C.glProgramUniformMatrix4fv(this.program, this.index, false, buf);
        }
    }
}
