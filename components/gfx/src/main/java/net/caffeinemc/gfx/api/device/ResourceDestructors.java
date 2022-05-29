package net.caffeinemc.gfx.api.device;

import net.caffeinemc.gfx.api.array.VertexArray;
import net.caffeinemc.gfx.api.buffer.Buffer;
import net.caffeinemc.gfx.api.pipeline.Pipeline;
import net.caffeinemc.gfx.api.shader.Program;
import net.caffeinemc.gfx.api.sync.Fence;
import net.caffeinemc.gfx.api.texture.Sampler;

public interface ResourceDestructors {
    void deletePipeline(Pipeline<?, ?> pipeline);

    void deleteSampler(Sampler sampler);

    void deleteVertexArray(VertexArray<?> array);

    void deleteBuffer(Buffer buffer);

    void deleteProgram(Program<?> program);
}
