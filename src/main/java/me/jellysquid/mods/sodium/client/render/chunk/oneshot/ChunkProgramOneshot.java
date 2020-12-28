package me.jellysquid.mods.sodium.client.render.chunk.oneshot;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderFogComponent;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import net.coderbot.iris.gl.program.ProgramUniforms;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.function.Function;

public class ChunkProgramOneshot extends ChunkProgram {
    // Uniform variable index for model offset
    private final int dModelOffset;
    private final int modelViewMatrixOffset;

    // Scratch buffer
    private final FloatBuffer uModelOffsetBuffer;

    private final ProgramUniforms irisProgramUniforms;

    public ChunkProgramOneshot(Identifier name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction, ProgramUniforms irisProgramUniforms) {
        super(name, handle, fogShaderFunction);

        this.dModelOffset = this.getUniformLocation("d_ModelOffset");
        this.modelViewMatrixOffset = this.getUniformLocation("u_ModelViewMatrix");
        this.uModelOffsetBuffer = MemoryUtil.memAllocFloat(4);
        this.irisProgramUniforms = irisProgramUniforms;
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

    @Override
    public int getUniformLocation(String name) {
        try {
            return super.getUniformLocation(name);
        } catch (NullPointerException e) {
            System.err.println(e.getMessage());
            return -1;
        }
    }

    @Override
    public void setup(MatrixStack modelView) {
        super.setup(modelView);

        irisProgramUniforms.update();

        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        modelView.peek().getModel().writeToBuffer(buffer);
        buffer.rewind();

        GL21.glUniformMatrix4fv(modelViewMatrixOffset, false, buffer);
    }
}
