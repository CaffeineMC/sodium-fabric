package me.jellysquid.mods.sodium.client.render.chunk.lists;

public interface ChunkRenderListIterator {
    int getGraphicsStateId();
    int getVisibleFaces();

    boolean hasNext();
    void advance();
}
