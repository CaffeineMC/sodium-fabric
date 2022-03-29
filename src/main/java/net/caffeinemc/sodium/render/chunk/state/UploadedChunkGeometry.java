package net.caffeinemc.sodium.render.chunk.state;

import net.caffeinemc.sodium.render.arena.BufferSegment;
import net.caffeinemc.sodium.render.buffer.VertexRange;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.terrain.quad.properties.ChunkMeshFace;

import java.util.List;

public final class UploadedChunkGeometry {
    public final BufferSegment segment;
    public final PackedModel[] models;

    public UploadedChunkGeometry(BufferSegment segment, List<ChunkModel> models) {
        this.segment = segment;
        this.models = new PackedModel[models.size()];

        for (int i = 0; i < models.size(); i++) {
            this.models[i] = new PackedModel(models.get(i));
        }
    }

    public void delete() {
        this.segment.delete();
    }

    public static final class PackedModel {
        public final ChunkRenderPass pass;
        public final long[] ranges;

        public final int visibilityBits;

        public PackedModel(ChunkModel model) {
            this.pass = model.getRenderPass();
            this.ranges = new long[ChunkMeshFace.COUNT];

            for (int i = 0; i < ChunkMeshFace.COUNT; i++) {
                var range = model.getModelRanges()[i];

                long packed;

                if (range == null) {
                    packed = VertexRange.NULL;
                } else {
                    packed = VertexRange.pack(range.firstVertex(), 6 * (range.vertexCount() >> 2));
                }

                this.ranges[i] = packed;
            }

            this.visibilityBits = model.getVisibilityBits();
        }
    }
}
