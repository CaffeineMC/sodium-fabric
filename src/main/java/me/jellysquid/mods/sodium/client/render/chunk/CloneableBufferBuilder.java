package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.vertex.BufferUploadData;

public interface CloneableBufferBuilder {
    BufferUploadData copyData();
}
