package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.SodiumHooks;
import org.lwjgl.opengl.GL20;

public class ChunkShaderCullingComponent implements ShaderComponent {
    private final int u_CullingEquation;

    public ChunkShaderCullingComponent(ChunkProgram program) {
        u_CullingEquation = program.getUniformLocation("u_CullingEquation");
    }

    @Override
    public void bind() {
        float[] cullingEquation = SodiumHooks.getCullingEquation.get();
        GL20.glUniform4f(u_CullingEquation, cullingEquation[0], cullingEquation[1], cullingEquation[2], cullingEquation[3]);
    }

    @Override
    public void unbind() {

    }

    @Override
    public void delete() {

    }
}
