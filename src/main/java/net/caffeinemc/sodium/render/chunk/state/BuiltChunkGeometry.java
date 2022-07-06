package net.caffeinemc.sodium.render.chunk.state;

import net.caffeinemc.sodium.render.buffer.VertexData;
import org.jetbrains.annotations.Nullable;

public record BuiltChunkGeometry(@Nullable VertexData vertices,
                                 ChunkPassModel[] models) {
    
    private static final BuiltChunkGeometry EMPTY_INSTANCE = new BuiltChunkGeometry(null, null);
    
    public static BuiltChunkGeometry empty() {
        return EMPTY_INSTANCE;
    }

    public void delete() {
        if (this.vertices != null) {
            this.vertices.delete();
        }
    }
}
