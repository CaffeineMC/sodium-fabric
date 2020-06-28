package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderFogComponent;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ChunkProgramMultiDraw extends ChunkProgram {
    private final int dModelOffset;

    public ChunkProgramMultiDraw(Identifier name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction) {
        super(name, handle, fogShaderFunction);

        this.dModelOffset = this.getAttributeLocation("d_ModelOffset");
    }

    public int getModelOffsetAttributeLocation() {
        return this.dModelOffset;
    }
}
