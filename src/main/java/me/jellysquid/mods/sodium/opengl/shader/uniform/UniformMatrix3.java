package me.jellysquid.mods.sodium.opengl.shader.uniform;

import org.joml.Matrix3f;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class UniformMatrix3 extends Uniform {
    private UniformMatrix3(int program, int index) {
        super(program, index);
    }

    public static UniformFactory<UniformMatrix3> of() {
        return UniformMatrix3::new;
    }

    public void set(Matrix3f matrix3) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buf = stack.callocFloat(12);
            matrix3.get(buf);

            GL45C.glProgramUniformMatrix3fv(this.program, this.index, false, buf);
        }
    }

    @Deprecated // Vanilla type
    public void set(net.minecraft.util.math.Matrix3f matrix3) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buf = stack.callocFloat(12);
            matrix3.writeColumnMajor(buf);

            GL45C.glProgramUniformMatrix3fv(this.program, this.index, false, buf);
        }
    }
}
