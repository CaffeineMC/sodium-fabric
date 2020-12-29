package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderFogComponent;
import net.coderbot.iris.gl.program.ProgramUniforms;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.nio.FloatBuffer;
import java.util.function.Function;

public class ChunkProgramMultiDraw extends ChunkProgram {
    private final int dModelOffset;
    private final int modelViewMatrixOffset;
    private final ProgramUniforms irisProgramUniforms;

    public ChunkProgramMultiDraw(Identifier name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction, ProgramUniforms irisProgramUniforms) {
        super(name, handle, fogShaderFunction);

        this.dModelOffset = this.getAttributeLocation("d_ModelOffset");
        this.modelViewMatrixOffset = this.getUniformLocation("u_ModelViewMatrix");
        this.irisProgramUniforms = irisProgramUniforms;
    }

    public int getModelOffsetAttributeLocation() {
        return this.dModelOffset;
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
