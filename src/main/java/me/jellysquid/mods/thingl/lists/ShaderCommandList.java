package me.jellysquid.mods.thingl.lists;

import me.jellysquid.mods.thingl.tessellation.Tessellation;

import java.util.function.Consumer;

public interface ShaderCommandList {
    void useTessellation(Tessellation tessellation, Consumer<TessellationCommandList> consumer);
}
