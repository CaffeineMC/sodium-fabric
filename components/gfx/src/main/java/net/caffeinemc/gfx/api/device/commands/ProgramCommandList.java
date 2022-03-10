package net.caffeinemc.gfx.api.device.commands;

import net.caffeinemc.gfx.api.array.VertexArray;

import java.util.function.Consumer;

public interface ProgramCommandList {
    <A extends Enum<A>> void useVertexArray(VertexArray<A> array, Consumer<RenderCommandList<A>> consumer);
}
