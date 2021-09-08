package me.jellysquid.mods.thingl.lists;

import me.jellysquid.mods.thingl.tessellation.GlTessellation;

import java.util.function.Consumer;

public interface ShaderCommandList {
    void useTessellation(GlTessellation tessellation, Consumer<TessellationCommandList> consumer);
}
