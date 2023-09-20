package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.Int2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.GroupBuilder.Quad;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.collections.BitArray;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * TODO: the sort algorithm uses a heuristic to avoid issues arising from
 * parallel planes of many quads. (see below) This heuristic is imperfect and
 * can cause some artifacts. A better approach would be a visible-only
 * topological sort. That's hard though, because it needs to handle arbitrary
 * amounts of (potentially unaligned) quads and run on every trigger event.
 * Possibly re-use of (incrementally constructed) graph structures is necessary?
 */
public class DynamicData extends MixedDirectionData {
    private final Quad[] quads;
    private final AccumulationGroup[] axisAlignedDistances;
    private final Int2ReferenceLinkedOpenHashMap<AccumulationGroup> unalignedDistances;

    DynamicData(ChunkSectionPos sectionPos,
            NativeBuffer buffer, VertexRange range, Quad[] quads,
            AccumulationGroup[] axisAlignedDistances,
            Int2ReferenceLinkedOpenHashMap<AccumulationGroup> unalignedDistances) {
        super(sectionPos, buffer, range);
        this.quads = quads;
        this.axisAlignedDistances = axisAlignedDistances;
        this.unalignedDistances = unalignedDistances;
    }

    @Override
    public SortType getSortType() {
        return SortType.DYNAMIC_ALL;
    }

    AccumulationGroup getGroupForNormal(NormalList normalList) {
        int groupBuilderKey = normalList.getGroupBuilderKey();
        if (groupBuilderKey < 0xFF) {
            if (this.axisAlignedDistances == null) {
                return null;
            }
            return this.axisAlignedDistances[groupBuilderKey];
        } else {
            if (this.unalignedDistances == null) {
                return null;
            }
            return this.unalignedDistances.get(groupBuilderKey);
        }
    }

    public AccumulationGroup[] getAxisAlignedDistances() {
        return this.axisAlignedDistances;
    }

    public Int2ReferenceLinkedOpenHashMap<AccumulationGroup> getUnalignedDistances() {
        return this.unalignedDistances;
    }

    private static float halfspace(Vector3fc planeAnchor, Vector3fc planeNormal, Vector3fc point) {
        return (point.x() - planeAnchor.x()) * planeNormal.x() +
                (point.y() - planeAnchor.y()) * planeNormal.y() +
                (point.z() - planeAnchor.z()) * planeNormal.z();
    }

    @Override
    public void sort(Vector3fc cameraPos) {
        int[] indexes = new int[this.quads.length];
        BitArray visible = new BitArray(this.quads.length);
        float[] centerDist = new float[this.quads.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = i;
            Quad quad = this.quads[i];
            if (halfspace(quad.center(), quad.normal(), cameraPos) > 0) {
                visible.set(i);
                centerDist[i] = cameraPos.distanceSquared(this.quads[i].center());
            }
        }

        IntArrays.quickSort(indexes, (a, b) -> {
            // returning -1 results in the ordering a, b, returning 1 produces b, a

            boolean aVisible = visible.get(a);
            boolean bVisible = visible.get(b);

            // compare only pairs where both are visible
            if (aVisible && bVisible) {
                var quadA = this.quads[a];
                var quadB = this.quads[b];
                var normalA = quadA.normal();
                var normalB = quadB.normal();

                // this is a heuristic that attempts to reduce the sorting mistakes that happen
                // when two parallel planes made up of many quads are seen from different
                // angles. For quads with equal normals it sorts them only by their
                // normal-relative distance to the camera. This eliminates some of wrong sorting
                // that happens in tangent directions.
                if (normalA.equals(normalB)) {
                    var centerA = quadA.center();
                    var centerB = quadB.center();
                    var cameraDistance = normalB.dot(cameraPos);
                    var result = Float.compare(Math.abs(cameraDistance - normalB.dot(centerB)),
                            Math.abs(cameraDistance - normalB.dot(centerA)));
                    if (result != 0) {
                        return result;
                    }
                }

                return Float.compare(centerDist[b], centerDist[a]);

            }

            // put invisible quads last
            if (aVisible) {
                return -1;
            }
            if (bVisible) {
                return 1;
            }

            return 0;
        });

        var intBuffer = this.buffer.getDirectBuffer().asIntBuffer();
        TranslucentData.writeVertexIndexes(intBuffer, indexes);
    }

    static DynamicData fromMesh(BuiltSectionMeshParts translucentMesh, NativeBuffer reuseBuffer,
            Vector3fc cameraPos, Quad[] quads, ChunkSectionPos sectionPos, GroupBuilder groupBuilder) {
        VertexRange range = TranslucentData.getUnassignedVertexRange(translucentMesh);

        if (reuseBuffer == null) {
            reuseBuffer = new NativeBuffer(TranslucentData.quadCountToIndexBytes(quads.length));
        }

        var dynamicData = new DynamicData(sectionPos,
                reuseBuffer, range, quads,
                groupBuilder.axisAlignedDistances, groupBuilder.unalignedDistances);
        dynamicData.sort(cameraPos);
        return dynamicData;
    }
}
