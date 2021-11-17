package me.jellysquid.mods.sodium.interop.vanilla.buffer;

import net.minecraft.client.render.VertexFormat;

public interface VertexBufferAccessor {

    int getIndexCount();

    VertexFormat.DrawMode getDrawMode();

    VertexFormat.IntType getIndexType();

    int getVertexBufferId();

    void invokeBindVertexArray();

    void invokeBind();
}
