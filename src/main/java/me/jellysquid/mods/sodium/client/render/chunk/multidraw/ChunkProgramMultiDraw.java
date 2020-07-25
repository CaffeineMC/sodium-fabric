package me.jellysquid.mods.sodium.client.render.chunk.multidraw;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgram;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkProgramComponentBuilder;
import net.minecraft.util.Identifier;

public class ChunkProgramMultiDraw extends ChunkProgram {
    private final int dModelOffset;

    public ChunkProgramMultiDraw(Identifier name, int handle, ChunkProgramComponentBuilder components) {
        super(name, handle, components);

        this.dModelOffset = this.getAttributeLocation("d_ModelOffset");
    }

    public int getModelOffsetAttributeLocation() {
        return this.dModelOffset;
    }
}
