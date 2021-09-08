package me.jellysquid.mods.thingl.tessellation;

import me.jellysquid.mods.thingl.device.CommandList;

public interface GlTessellation {
    void delete(CommandList commandList);

    void bind(CommandList commandList);

    GlPrimitiveType getPrimitiveType();
}
