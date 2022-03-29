package net.caffeinemc.sodium.render.chunk.state;

import net.caffeinemc.sodium.render.buffer.VertexData;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public record BuiltChunkGeometry(@Nullable VertexData vertices,
                                 List<ChunkModel> models) {
    public static BuiltChunkGeometry empty() {
        return new BuiltChunkGeometry(null, Collections.emptyList());
    }

    public void delete() {
        if (this.vertices != null) {
            this.vertices.delete();
        }
    }
}
