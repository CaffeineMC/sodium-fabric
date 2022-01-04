package me.jellysquid.mods.sodium.opengl.shader.uniform;

import net.minecraft.util.math.Vec3f;
import org.lwjgl.opengl.GL45C;

public final class UniformFloatArray extends Uniform {
    private final int length;

    private UniformFloatArray(int program, int index, int length) {
        super(program, index);

        this.length = length;
    }

    public static UniformFactory<UniformFloatArray> ofSize(int length) {
        return (program, index) -> new UniformFloatArray(program, index, length);
    }

    public void setFloat(float value) {
        this.checkLength(1);
        GL45C.glProgramUniform1f(this.program, this.index, value);
    }

    public void setFloat(float x, float y) {
        this.checkLength(2);
        GL45C.glProgramUniform2f(this.program, this.index, x, y);
    }

    public void setFloat(float x, float y, float z) {
        this.checkLength(3);
        GL45C.glProgramUniform3f(this.program, this.index, x, y, z);
    }

    public void setFloat(float x, float y, float z, float w) {
        this.checkLength(4);
        GL45C.glProgramUniform4f(this.program, this.index, x, y, z, w);
    }

    public void setFloat(float[] values) {
        this.checkLength(values.length);

        // TODO: Use FloatBuffer instead of float[] due to overhead
        switch (this.length) {
            case 1 -> GL45C.glProgramUniform1fv(this.program, this.index, values);
            case 2 -> GL45C.glProgramUniform2fv(this.program, this.index, values);
            case 3 -> GL45C.glProgramUniform3fv(this.program, this.index, values);
            case 4 -> GL45C.glProgramUniform4fv(this.program, this.index, values);
            default -> throw new UnsupportedOperationException();
        }
    }

    private void checkLength(int capacity) {
        if (this.length != capacity) {
            throw new IllegalArgumentException("Expected %s values, found %s values".formatted(this.length, capacity));
        }
    }

    @Deprecated // Vanilla type
    public void setFloat(Vec3f vec) {
        this.setFloat(vec.getX(), vec.getY(), vec.getZ());
    }
}
