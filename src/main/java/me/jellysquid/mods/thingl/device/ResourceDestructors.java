package me.jellysquid.mods.thingl.device;

import me.jellysquid.mods.thingl.array.VertexArray;
import me.jellysquid.mods.thingl.buffer.Buffer;
import me.jellysquid.mods.thingl.shader.Program;
import me.jellysquid.mods.thingl.shader.Shader;
import me.jellysquid.mods.thingl.sync.Fence;
import me.jellysquid.mods.thingl.tessellation.Tessellation;
import me.jellysquid.mods.thingl.texture.Sampler;
import me.jellysquid.mods.thingl.texture.Texture;

public interface ResourceDestructors {
    void deleteBuffer(Buffer buffer);

    void deleteTessellation(Tessellation tessellation);

    void deleteVertexArray(VertexArray vertexArray);

    void deleteProgram(Program<?> program);

    void deleteShader(Shader shader);

    void deleteSampler(Sampler sampler);

    void deleteTexture(Texture texture);

    void deleteFence(Fence fence);
}
