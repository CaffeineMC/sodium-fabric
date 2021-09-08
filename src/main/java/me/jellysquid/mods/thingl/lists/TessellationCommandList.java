package me.jellysquid.mods.thingl.lists;

import me.jellysquid.mods.thingl.tessellation.IndexType;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;

public interface TessellationCommandList {
    void multiDrawElementsBaseVertex(PointerBuffer pointerBuffer, IntBuffer countBuffer, IntBuffer baseVertexBuffer, IndexType indexType);
}
