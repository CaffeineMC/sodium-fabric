package me.jellysquid.mods.sodium.client.render.chunk.format;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import org.apache.commons.lang3.Validate;

import java.util.Map;

public record ChunkMesh(ChunkMeshBuffers<?> buffers,
                        Map<ModelQuadFacing, MeshRange> parts) {
    public ChunkMesh {
        Validate.isTrue(!parts.isEmpty());
    }

    @Deprecated
    public void delete() {
        this.buffers.delete();
    }
}
