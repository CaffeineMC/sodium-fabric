package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.minecraft.core.SectionPos;

import java.nio.IntBuffer;
import java.util.function.IntConsumer;

/**
 * Static topo acyclic sorting uses the topo sorting algorithm but only if it's
 * possible to sort without dynamic triggering, meaning the sort order never
 * needs to change.
 */
public class StaticTopoData extends MixedDirectionData {
    private Sorter sorterOnce;

    StaticTopoData(SectionPos sectionPos, int vertexCount, int quadCount) {
        super(sectionPos, vertexCount, quadCount);
    }

    @Override
    public SortType getSortType() {
        return SortType.STATIC_TOPO;
    }

    @Override
    public Sorter getSorter() {
        var sorter = this.sorterOnce;
        if (sorter == null) {
            throw new IllegalStateException("Sorter already used!");
        }
        this.sorterOnce = null;
        return sorter;
    }

    private record QuadIndexConsumerIntoBuffer(IntBuffer buffer) implements IntConsumer {
        @Override
        public void accept(int value) {
            TranslucentData.writeQuadVertexIndexes(this.buffer, value);
        }
    }

    public static StaticTopoData fromMesh(int vertexCount, TQuad[] quads, SectionPos sectionPos) {
        var sorter = new StaticSorter(quads.length);
        var indexWriter = new QuadIndexConsumerIntoBuffer(sorter.getIntBuffer());

        if (!TopoGraphSorting.topoGraphSort(indexWriter, quads, null, null)) {
            sorter.getIndexBuffer().free();
            return null;
        }

        var staticTopoData = new StaticTopoData(sectionPos, vertexCount, quads.length);
        staticTopoData.sorterOnce = sorter;
        return staticTopoData;
    }
}
