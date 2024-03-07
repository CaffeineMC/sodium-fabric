package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.gl.util.VertexRange;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.minecraft.core.SectionPos;

import java.nio.IntBuffer;
import java.util.function.IntConsumer;

/**
 * Static topo acyclic sorting uses the topo sorting algorithm but only if it's
 * possible to sort without dynamic triggering, meaning the sort order never
 * needs to change.
 */
public class StaticTopoAcyclicData extends MixedDirectionData {
    StaticTopoAcyclicData(SectionPos sectionPos, NativeBuffer buffer, VertexRange range) {
        super(sectionPos, buffer, range);
    }

    @Override
    public SortType getSortType() {
        return SortType.STATIC_TOPO;
    }

    private record QuadIndexConsumerIntoBuffer(IntBuffer buffer) implements IntConsumer {
        @Override
        public void accept(int value) {
            TranslucentData.writeQuadVertexIndexes(this.buffer, value);
        }
    }


    public static StaticTopoAcyclicData fromMesh(BuiltSectionMeshParts translucentMesh,
            TQuad[] quads, SectionPos sectionPos, NativeBuffer buffer) {
        VertexRange range = TranslucentData.getUnassignedVertexRange(translucentMesh);
        var indexWriter = new QuadIndexConsumerIntoBuffer(buffer.getDirectBuffer().asIntBuffer());

        if (!TopoGraphSorting.topoGraphSort(indexWriter, quads, null, null)) {
            return null;
        }

        return new StaticTopoAcyclicData(sectionPos, buffer, range);
    }
}
