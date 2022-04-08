package net.caffeinemc.sodium.render.chunk.state;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.caffeinemc.sodium.render.arena.BufferSegment;
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

            var ranges = new LongArrayList(ChunkMeshFace.COUNT);

            for (int faceIndex = 0; faceIndex < ChunkMeshFace.COUNT; faceIndex++) {
                var range = model.getModelRanges()[faceIndex];

                if (range == null) {
                    continue;
                }

                long packed = ModelPart.pack(1 << faceIndex, 6 * (range.vertexCount() >> 2), range.firstVertex());
                ranges.add(packed);
            }

            this.ranges = ranges.toLongArray();
            this.visibilityBits = model.getVisibilityBits();
        }
    }

    public static class ModelPart {
        public static long pack(int face, int vertexCount, int firstVertex) {
            long packed = 0L;
            packed |= (face & 0xFFL) << 0;
            packed |= (vertexCount & 0xFFFFFFFL) << 8;
            packed |= (firstVertex & 0xFFFFFFFL) << 36;

            return packed;
        }

        public static int unpackFace(long packed) {
            return (int) ((packed >>> 0) & 0xFFL);
        }

        public static int unpackVertexCount(long packed) {
            return (int) ((packed >>> 8) & 0xFFFFFFFL);
        }

        public static int unpackFirstVertex(long packed) {
            return (int) ((packed >>> 36) & 0xFFFFFFFL);
        }
    }
}
