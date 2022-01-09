package me.jellysquid.mods.sodium.opengl.pipeline;

import me.jellysquid.mods.sodium.opengl.array.VertexArray;
import me.jellysquid.mods.sodium.opengl.shader.Program;
import me.jellysquid.mods.sodium.opengl.types.RenderState;

public interface Pipeline<PROGRAM, ARRAY extends Enum<ARRAY>> {
    RenderState getState();

    Program<PROGRAM> getProgram();

    VertexArray<ARRAY> getVertexArray();
}
