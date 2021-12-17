package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.format.MeshRange;

import java.util.Map;

public record ChunkGraphicsState<E extends Enum<E>>(
        Map<E, GlBufferSegment> buffers,
        Map<ModelQuadFacing, MeshRange> parts) {

    public void delete() {
        for (var segment : this.buffers.values()) {
            segment.delete();
        }
    }

    public MeshRange getRange(ModelQuadFacing facing) {
        return this.parts.get(facing);
    }

    public GlBufferSegment getBuffer(E target) {
        return this.buffers.get(target);
    }
}
