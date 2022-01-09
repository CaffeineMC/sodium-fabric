package me.jellysquid.mods.sodium.opengl.shader;

import me.jellysquid.mods.sodium.opengl.array.VertexArray;
import me.jellysquid.mods.sodium.opengl.array.DrawCommandList;

import java.util.function.Consumer;

public interface ProgramCommandList {
    <A extends Enum<A>> void useVertexArray(VertexArray<A> array, Consumer<DrawCommandList<A>> consumer);
}
