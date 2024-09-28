package net.caffeinemc.mods.sodium.client.render.chunk.data;

import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;

public class BuiltSectionMeshParts {
    private final int[] vertexSegments;
    private final NativeBuffer buffer;

    public BuiltSectionMeshParts(NativeBuffer buffer, int[] vertexCounts) {
        this.vertexSegments = vertexCounts;
        this.buffer = buffer;
    }

    public NativeBuffer getVertexData() {
        return this.buffer;
    }

    public int[] getVertexSegments() {
        return this.vertexSegments;
    }

    public int[] computeVertexCounts() {
        var vertexCounts = new int[ModelQuadFacing.COUNT];

        for (int i : this.vertexSegments) {
            var count = SectionRenderDataUnsafe.decodeVertexCount(i);

            // it's important to only write non-zero vertex counts since the decoded facing is wrong if the count is zero
            // (the whole segment is just zero, which decodes to the first facing, but it's not actually that facing, just no vertexes)
            if (count > 0) {
                vertexCounts[SectionRenderDataUnsafe.decodeFacing(i)] = count;
            }
        }

        return vertexCounts;
    }
}
