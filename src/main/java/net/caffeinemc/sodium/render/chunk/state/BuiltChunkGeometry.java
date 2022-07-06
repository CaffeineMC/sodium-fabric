package net.caffeinemc.sodium.render.chunk.state;

import net.caffeinemc.sodium.render.buffer.VertexData;
import org.jetbrains.annotations.Nullable;

public record BuiltChunkGeometry(@Nullable VertexData vertices,
                                 ChunkPassModel[] models) {
    
    private static final ChunkPassModel[] EMPTY_MODELS = new ChunkPassModel[0];
    
    public static BuiltChunkGeometry empty() {
        return new BuiltChunkGeometry(null, EMPTY_MODELS);
    }

    public void delete() {
        if (this.vertices != null) {
            this.vertices.delete();
        }
    }
}
