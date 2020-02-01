package me.jellysquid.mods.sodium.client.render.chunk;

import net.minecraft.client.render.RenderLayer;

public interface ExtendedBuiltChunk {
    VertexBufferWithArray getBufferWithArray(RenderLayer layer);

    boolean usesVAORendering();
}
