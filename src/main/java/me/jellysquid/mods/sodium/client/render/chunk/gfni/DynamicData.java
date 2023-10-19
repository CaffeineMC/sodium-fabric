package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.nio.IntBuffer;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import net.minecraft.util.math.ChunkSectionPos;

public class DynamicData extends MixedDirectionData {
    private final TQuad[] quads;
    boolean GFNITrigger = true;
    boolean angleTrigger = false;
    boolean turnAngleTriggerOn = false;
    boolean turnGFNITriggerOff = false;
    private boolean pendingTriggerIsAngle;
    private TranslucentGeometryCollector collector;
    private Object2ReferenceOpenHashMap<Vector3fc, double[]> distancesByNormal;

    DynamicData(ChunkSectionPos sectionPos,
            NativeBuffer buffer, VertexRange range, TQuad[] quads,
            TranslucentGeometryCollector collector,
            Object2ReferenceOpenHashMap<Vector3fc, double[]> distancesByNormal) {
        super(sectionPos, buffer, range);
        this.quads = quads;
        this.collector = collector;
        this.distancesByNormal = distancesByNormal;
    }

    @Override
    public SortType getSortType() {
        return SortType.DYNAMIC_ALL;
    }

    private static final Collection<Pair<Integer, Long>> sortTimingData = new ConcurrentLinkedDeque<>();

    private static void updateTimingData(int quadCount, long sortTime) {
        sortTimingData.add(Pair.of(quadCount, sortTime));
        if (sortTimingData.size() > 3000) {
            synchronized (sortTimingData) {
                if (sortTimingData.size() <= 3000) {
                    return;
                }
                var totalQuads = sortTimingData.stream().mapToLong(Pair::getLeft).sum();
                var totalTime = sortTimingData.stream().mapToLong(Pair::getRight).sum();
                var averageTime = totalTime / (double) sortTimingData.size();
                var averageQuads = totalQuads / (double) sortTimingData.size();
                System.out.println("Average sort time: " + averageTime + "ms for " + averageQuads + " quads");

                // print whole data as csv
                var builder = new StringBuilder();
                builder.append("\nquads,time\n");
                for (var pair : sortTimingData) {
                    builder.append(pair.getLeft()).append(",").append(pair.getRight()).append(";");
                }
                System.out.println(builder.toString());

                sortTimingData.clear();
            }
        }
    }

    private long sortStart;

    void startSortTimer() {
        this.sortStart = System.nanoTime();
    }

    void endSortTimer() {
        // var sortTime = System.nanoTime() - this.sortStart;
        // if (this instanceof PresentTranslucentData present) {
        //     updateTimingData(present.getQuadLength(), sortTime);
        // }
    }

    @Override
    public boolean prepareTrigger(boolean isAngleTrigger) {
        // if an angle trigger was scheduled but isn't needed, return true to signal
        // removal from angle triggering
        if (isAngleTrigger && !this.angleTrigger) {
            return true;
        }

        this.pendingTriggerIsAngle = isAngleTrigger;
        return false;
    }

    @Override
    public void sortOnTrigger(Vector3fc cameraPos) {
        this.sortWithTiming(cameraPos, this.pendingTriggerIsAngle);
    }

    private void sortWithTiming(Vector3fc cameraPos, boolean isAngleTrigger) {
        this.startSortTimer();
        this.sort(cameraPos, isAngleTrigger);
        this.endSortTimer();
    }

    private void sort(Vector3fc cameraPos, boolean isAngleTrigger) {
        // uses a topo sort or a distance sort depending on what is enabled
        IntBuffer indexBuffer = this.buffer.getDirectBuffer().asIntBuffer();

        if (this.GFNITrigger && !isAngleTrigger) {
            if (ComplexSorting.topoSortDepthFirstCyclic(indexBuffer, this.quads, this.distancesByNormal, cameraPos)) {
                // disable distance sorting because topo sort seems to be possible.
                // removal from angle triggering happens automatically by setting this to false.
                this.angleTrigger = false;
                return;
            } else {
                // topo sort failure, the topo sort algorithm doesn't work on all cases
                System.out.println("Failed to sort at " + this.sectionPos);

                // TODO: implement different give-up heuristic. this gives up after the second
                // failure.
                if (this.angleTrigger) {
                    // turn off if currently on and signal change
                    if (this.GFNITrigger) {
                        this.GFNITrigger = false;
                        this.turnGFNITriggerOff = true;
                    }
                }
                if (!this.angleTrigger) {
                    this.angleTrigger = true;
                    this.turnAngleTriggerOn = true;
                }
            }
        }
        if (this.angleTrigger) {
            indexBuffer.rewind();
            ComplexSorting.distanceSortDirect(indexBuffer, this.quads, cameraPos);
            return;
        }
    }

    public boolean hasTriggerChanges() {
        return this.turnAngleTriggerOn || this.turnGFNITriggerOff;
    }

    TranslucentGeometryCollector getCollector() {
        return this.collector;
    }

    void deleteCollector() {
        this.collector = null;
    }

    static DynamicData fromMesh(BuiltSectionMeshParts translucentMesh,
            Vector3fc cameraPos, TQuad[] quads, ChunkSectionPos sectionPos, TranslucentGeometryCollector collector) {
        // prepare accumulation groups for GFNI integration and copy
        var size = 0;
        if (collector.axisAlignedDistances != null) {
            size += Integer.bitCount(collector.alignedNormalBitmap);
        }
        if (collector.unalignedDistances != null) {
            size += collector.unalignedDistances.size();
        }
        var distancesByNormal = new Object2ReferenceOpenHashMap<Vector3fc, double[]>(size);
        if (collector.axisAlignedDistances != null) {
            for (int direction = 0; direction < ModelQuadFacing.DIRECTIONS; direction++) {
                var accGroup = collector.axisAlignedDistances[direction];
                if (accGroup != null) {
                    accGroup.prepareIntegration();
                    distancesByNormal.put(accGroup.normal, accGroup.facePlaneDistances);
                }
            }
        }
        if (collector.unalignedDistances != null) {
            for (var accGroup : collector.unalignedDistances.values()) {
                // TODO: get rid of collector key and just use the normal vector's hash code
                accGroup.prepareIntegration();
                distancesByNormal.put(accGroup.normal, accGroup.facePlaneDistances);
            }
        }

        VertexRange range = TranslucentData.getUnassignedVertexRange(translucentMesh);
        var buffer = new NativeBuffer(TranslucentData.quadCountToIndexBytes(quads.length));

        var dynamicData = new DynamicData(sectionPos, buffer, range, quads, collector, distancesByNormal);

        dynamicData.sortWithTiming(cameraPos, false);

        return dynamicData;
    }
}
