package me.jellysquid.mods.sodium.client.render.chunk.oneshot;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderFogComponent;

import net.minecraft.util.Identifier;

import net.coderbot.iris.gl.program.ProgramUniforms;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.function.Function;

public class ChunkProgramOneshot extends ChunkProgram {
    // Uniform variable index for model offset
    private final int dModelOffset;

    // Scratch buffer
    private final FloatBuffer uModelOffsetBuffer;

    public ChunkProgramOneshot(Identifier name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction, @Nullable ProgramUniforms irisProgramUniforms) {
        super(name, handle, fogShaderFunction, irisProgramUniforms);

        this.dModelOffset = this.getUniformLocation("d_ModelOffset");
        this.uModelOffsetBuffer = MemoryUtil.memAllocFloat(4);
    }

    public void setModelOffset(float x, float y, float z) {
        FloatBuffer buf = this.uModelOffsetBuffer;
        buf.put(0, x);
        buf.put(1, y);
        buf.put(2, z);

        GL20.glUniform4fv(this.dModelOffset, buf);
    }

    @Override
    public void delete() {
        super.delete();

        MemoryUtil.memFree(this.uModelOffsetBuffer);
    }
}
