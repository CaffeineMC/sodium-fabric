package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderFogComponent;
import net.coderbot.iris.gl.program.ProgramUniforms;
import org.jetbrains.annotations.Nullable;
import net.minecraft.util.Identifier;
import java.util.function.Function;

public class ChunkProgramMultiDraw extends ChunkProgram {
    private final int dModelOffset;

    public ChunkProgramMultiDraw(Identifier name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction, @Nullable ProgramUniforms irisProgramUniforms) {
        super(name, handle, fogShaderFunction, irisProgramUniforms);

        this.dModelOffset = this.getAttributeLocation("d_ModelOffset");
    }

    public int getModelOffsetAttributeLocation() {
        return this.dModelOffset;
    }
}
