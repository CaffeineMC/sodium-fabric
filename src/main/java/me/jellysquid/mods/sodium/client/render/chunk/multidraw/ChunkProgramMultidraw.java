package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderFogComponent;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.function.Function;

public class ChunkProgramMultidraw extends ChunkProgram {
    private final int uModelOffsetsLocation;

    public ChunkProgramMultidraw(Identifier name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction) {
        super(name, handle, fogShaderFunction);

        this.uModelOffsetsLocation = this.getUniformLocation("u_ModelOffsets");
    }

    public void uploadModelOffsetUniforms(FloatBuffer buffer) {
        GL20.glUniform3fv(this.uModelOffsetsLocation, buffer);
    }
}
