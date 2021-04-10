package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderFogComponent;
import net.coderbot.iris.gl.program.ProgramUniforms;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

import java.nio.FloatBuffer;
import java.util.function.Function;

public class ChunkProgramMultiDraw extends ChunkProgram {
    private final int dModelOffset;
    private final int modelViewMatrixOffset;
    private final int normalMatrixOffset;
    @Nullable
    private final ProgramUniforms irisProgramUniforms;

    public ChunkProgramMultiDraw(Identifier name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction, @Nullable  ProgramUniforms irisProgramUniforms) {
        super(name, handle, fogShaderFunction);

        this.dModelOffset = this.getAttributeLocation("d_ModelOffset");
        this.modelViewMatrixOffset = this.getUniformLocation("u_ModelViewMatrix");
        this.normalMatrixOffset = this.getUniformLocation("u_NormalMatrix");
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
    public void setup(MatrixStack matrixStack, float modelScale, float textureScale) {
        super.setup(matrixStack, modelScale, textureScale);

        if (irisProgramUniforms != null) {
            irisProgramUniforms.update();
        }

        Matrix4f modelViewMatrix = matrixStack.peek().getModel();
        Matrix4f normalMatrix = matrixStack.peek().getModel().copy();
        normalMatrix.invert();
        normalMatrix.transpose();

        uniformMatrix(modelViewMatrixOffset, modelViewMatrix);
        uniformMatrix(normalMatrixOffset, normalMatrix);
    }

    private void uniformMatrix(int location, Matrix4f matrix) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        matrix.writeToBuffer(buffer);
        buffer.rewind();

        GL21.glUniformMatrix4fv(location, false, buffer);
    }
}
