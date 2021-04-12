package me.jellysquid.mods.sodium.client.gl.shader;

public class ShaderBindingPoint {
    private final int genericAttributeIndex;

    public ShaderBindingPoint(int genericAttributeIndex) {
        this.genericAttributeIndex = genericAttributeIndex;
    }

    public int getGenericAttributeIndex() {
        return genericAttributeIndex;
    }
}
