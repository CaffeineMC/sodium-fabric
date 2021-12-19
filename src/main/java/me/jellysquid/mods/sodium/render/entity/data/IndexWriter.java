package me.jellysquid.mods.sodium.render.entity.data;

public interface IndexWriter {
    void writeIndices(long ptr, int startIdx, int vertsPerPrim);
}
