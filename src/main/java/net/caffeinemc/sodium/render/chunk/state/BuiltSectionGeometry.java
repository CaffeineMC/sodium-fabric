package net.caffeinemc.sodium.render.chunk.state;

import net.caffeinemc.sodium.render.buffer.VertexData;
import org.jetbrains.annotations.Nullable;

public record BuiltSectionGeometry(@Nullable VertexData vertices,
                                   SectionPassModel[] models) {
    
    private static final BuiltSectionGeometry EMPTY_INSTANCE = new BuiltSectionGeometry(null, null);
    
    public static BuiltSectionGeometry empty() {
        return EMPTY_INSTANCE;
    }

    public void delete() {
        if (this.vertices != null) {
            this.vertices.delete();
        }
    }
}
