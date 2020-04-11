package me.jellysquid.mods.sodium.client.gl.tessellation;

public interface GlTessellation {
    void bind();

    void draw(int mode);

    void unbind();

    void delete();
}
