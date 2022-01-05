package me.jellysquid.mods.sodium.opengl.shader.uniform;

import me.jellysquid.mods.sodium.opengl.util.MemCmp;
import net.minecraft.util.math.Vec3f;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public final class UniformFloatArray extends Uniform {
    private final int length;
    private FloatBuffer currentValue;
    private FloatBuffer temp;

    private UniformFloatArray(int program, int index, int length) {
        super(program, index);

        this.length = length;
        this.currentValue = createBuffer(length);
        this.temp = createBuffer(length);

        GL45C.glGetUniformfv(program, index, this.currentValue);
    }

    public static UniformFactory<UniformFloatArray> ofSize(int length) {
        return (program, index) -> new UniformFloatArray(program, index, length);
    }

    private static FloatBuffer createBuffer(int length) {
        return ByteBuffer.allocateDirect(length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    public void setFloats(float x) {
        this.checkLength(1);
        this.temp.put(0, x);

        if (this.compareAndSwap()) {
            GL45C.glProgramUniform1fv(this.program, this.index, this.currentValue);
        }
    }

    public void setFloats(float x, float y) {
        this.checkLength(2);
        this.temp.put(0, x);
        this.temp.put(1, y);

        if (this.compareAndSwap()) {
            GL45C.glProgramUniform2fv(this.program, this.index, this.currentValue);
        }
    }

    public void setFloats(float x, float y, float z) {
        this.checkLength(3);
        this.temp.put(0, x);
        this.temp.put(1, y);
        this.temp.put(2, z);

        if (this.compareAndSwap()) {
            GL45C.glProgramUniform3fv(this.program, this.index, this.currentValue);
        }
    }

    public void setFloats(float x, float y, float z, float w) {
        this.checkLength(4);
        this.temp.put(0, x);
        this.temp.put(1, y);
        this.temp.put(2, z);
        this.temp.put(3, w);

        if (this.compareAndSwap()) {
            GL45C.glProgramUniform4fv(this.program, this.index, this.currentValue);
        }
    }

    public void setFloats(float[] values) {
        this.setFloats(values, values.length);
    }

    private void setFloats(float[] values, int count) {
        this.checkLength(count);
        this.temp.put(0, values);

        if (this.compareAndSwap()) {
            switch (this.length) {
                case 1 -> GL45C.glProgramUniform1fv(this.program, this.index, this.currentValue);
                case 2 -> GL45C.glProgramUniform2fv(this.program, this.index, this.currentValue);
                case 3 -> GL45C.glProgramUniform3fv(this.program, this.index, this.currentValue);
                case 4 -> GL45C.glProgramUniform4fv(this.program, this.index, this.currentValue);
                default -> throw new UnsupportedOperationException();
            }
        }
    }

    private boolean compareAndSwap() {
        if (!MemCmp.compare(this.currentValue, this.temp)) {
            var currentValue = this.currentValue;
            this.currentValue = this.temp;
            this.temp = currentValue;

            return true;
        }

        return false;
    }

    private void checkLength(int capacity) {
        if (this.length != capacity) {
            throw new IllegalArgumentException("Expected %s values, found %s values".formatted(this.length, capacity));
        }
    }

    @Deprecated // Vanilla type
    public void setFloats(Vec3f vec) {
        this.setFloats(vec.getX(), vec.getY(), vec.getZ());
    }
}
