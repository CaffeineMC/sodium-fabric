package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderFogComponent;
import net.coderbot.iris.gl.program.ProgramUniforms;
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
    private final ProgramUniforms irisProgramUniforms;

    public ChunkProgramMultiDraw(Identifier name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction, ProgramUniforms irisProgramUniforms) {
        super(name, handle, fogShaderFunction);

        this.dModelOffset = this.getAttributeLocation("d_ModelOffset");
        this.modelViewMatrixOffset = this.getUniformLocation("u_ModelViewMatrix");
        this.normalMatrixOffset = this.getUniformLocation("u_NormalMatrix");
        this.irisProgramUniforms = irisProgramUniforms;
    }

    /*private static void setupAttributes(int programHandle) {
        // TODO: Properly add these attributes into the vertex format

        float blockId = -1.0F;

        setupAttribute(programHandle, "mc_Entity", blockId, -1.0F, -1.0F, -1.0F);
        setupAttribute(programHandle, "mc_midTexCoord", 0.0F, 0.0F, 0.0F, 0.0F);
        setupAttribute(programHandle, "at_tangent", 1.0F, 0.0F, 0.0F, 1.0F);
    }

    private static void setupAttribute(int programHandle, String name, float v0, float v1, float v2, float v3) {
        int location = GL20.glGetAttribLocation(programHandle, name);

        if (location != -1) {
            GL20.glVertexAttrib4f(location, v0, v1, v2, v3);
        }
    }*/

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

        //setupAttributes(this.handle());

        irisProgramUniforms.update();

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
