package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;

public interface ShaderComponent {
    void bind();
    void unbind();
    void delete();

    interface Factory<C extends ShaderComponent, S extends GlProgram> {
        C create(S shader);
    }
}
