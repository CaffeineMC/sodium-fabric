package me.jellysquid.mods.sodium.opengl.shader;

import net.minecraft.util.Identifier;

public interface Program<T> {
    @Deprecated(forRemoval = true)
    static ProgramImpl.Builder builder(Identifier identifier) {
        return ProgramImpl.builder(identifier);
    }

    T getInterface();

    @Deprecated(forRemoval = true)
    void bind();

    @Deprecated(forRemoval = true)
    void unbind();

    @Deprecated(forRemoval = true)
    void delete();
}
