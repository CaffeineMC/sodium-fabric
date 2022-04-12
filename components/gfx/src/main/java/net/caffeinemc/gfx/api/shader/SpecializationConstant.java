package net.caffeinemc.gfx.api.shader;

public class SpecializationConstant {
    private final int index;
    private final int value;

    private SpecializationConstant(int index, int value) {
        this.index = index;
        this.value = value;
    }

    public static SpecializationConstant ofInt(int index, int value) {
        return new SpecializationConstant(index, value);
    }

    public static SpecializationConstant ofFloat(int index, float value) {
        return new SpecializationConstant(index, Float.floatToRawIntBits(value));
    }

    public static SpecializationConstant ofBool(int index, boolean value) {
        return new SpecializationConstant(index, value ? 1 : 0);
    }

    public int index() {
        return this.index;
    }

    public int value() {
        return this.value;
    }
}
