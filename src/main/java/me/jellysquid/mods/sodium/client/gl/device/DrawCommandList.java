package me.jellysquid.mods.sodium.client.gl.device;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlPrimitiveType;
import net.minecraft.client.render.VertexFormat;

import java.nio.IntBuffer;

public interface DrawCommandList extends AutoCloseable {
    void multiDrawArrays(IntBuffer first, IntBuffer count);
    void multiDrawElementArrays(IntBuffer first, RenderSystem.IndexBuffer indexBuffer, IntBuffer count);
    void multiDrawElementArrays(IntBuffer first, VertexFormat.IntType intType, IntBuffer count);

    void multiDrawArraysIndirect(long pointer, int count, int stride);
    void multiDrawElementArraysIndirect(long pointer, RenderSystem.IndexBuffer indexBuffer, int count, int stride);
    void multiDrawElementArraysIndirect(long pointer, VertexFormat.IntType intType, int count, int stride);

    void endTessellating();

    void flush();

    @Override
    default void close() {
        this.flush();
    }
}
