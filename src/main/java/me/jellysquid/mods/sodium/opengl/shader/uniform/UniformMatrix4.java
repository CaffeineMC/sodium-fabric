package me.jellysquid.mods.sodium.opengl.shader.uniform;

import me.jellysquid.mods.sodium.opengl.util.MemCmp;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL45C;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class UniformMatrix4 extends Uniform {
    private FloatBuffer currentValue = createBuffer();
    private FloatBuffer temp = createBuffer();

    private UniformMatrix4(int program, int index) {
        super(program, index);

        GL45C.glGetUniformfv(program, index, this.currentValue);
    }

    public static UniformFactory<UniformMatrix4> of() {
        return UniformMatrix4::new;
    }

    private static FloatBuffer createBuffer() {
        return ByteBuffer.allocateDirect(16 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    public void set(Matrix4f matrix4) {
        matrix4.get(this.temp);
        this.compareAndSwap();
    }

    @Deprecated // vanilla type
    public void set(net.minecraft.util.math.Matrix4f matrix4) {
        matrix4.writeColumnMajor(this.temp);
        this.compareAndSwap();
    }

    private void compareAndSwap() {
        if (!MemCmp.compare(this.currentValue, this.temp)) {
            var prev = this.currentValue;
            this.currentValue = this.temp;
            this.temp = prev;

            GL45C.glProgramUniformMatrix4fv(this.program, this.index, false, this.currentValue);
        }
    }
}
