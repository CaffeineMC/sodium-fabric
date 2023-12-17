package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data;

import java.nio.IntBuffer;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TopoGraphSorting;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.sorting.MergeSort;
import net.minecraft.util.math.ChunkSectionPos;

public class TopoSortDynamicData extends DynamicData {
    private final TQuad[] quads;
    private boolean GFNITrigger = true;
    private boolean directTrigger = false;
    private boolean turnGFNITriggerOff = false;
    private boolean turnDirectTriggerOn = false;
    private boolean turnDirectTriggerOff = false;
    private double directTriggerKey = -1;
    private int consecutiveTopoSortFailures = 0;
    private boolean pendingTriggerIsAngle;
    private Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal;
    private int[] distanceSortIndexes;

    private static final int MAX_TOPO_SORT_QUADS = 1000;
    private static final int MAX_TOPO_SORT_TIME_NS = 1_000_000;
    private static final int MAX_FAILING_TOPO_SORT_TIME_NS = 750_000;
    private static final int MAX_TOPO_SORT_PATIENT_TIME_NS = 250_000;
    private static final int PATIENT_TOPO_ATTEMPTS = 5;
    private static final int REGULAR_TOPO_ATTEMPTS = 2;

    private TopoSortDynamicData(ChunkSectionPos sectionPos,
            NativeBuffer buffer, VertexRange range, TQuad[] quads,
            TranslucentGeometryCollector collector,
            Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal) {
        super(sectionPos, buffer, range, collector);
        this.quads = quads;
        this.distancesByNormal = distancesByNormal;
    }

    public boolean GFNITriggerEnabled() {
        return this.GFNITrigger;
    }

    public boolean directTriggerEnabled() {
        return this.directTrigger;
    }

    public void clearTriggerChanges() {
        this.turnGFNITriggerOff = false;
        this.turnDirectTriggerOn = false;
        this.turnDirectTriggerOff = false;
    }

    private void turnGFNITriggerOff() {
        if (this.GFNITrigger) {
            this.GFNITrigger = false;
            this.turnGFNITriggerOff = true;
        }
    }

    private void turnDirectTriggerOn() {
        if (!this.directTrigger) {
            this.directTrigger = true;
            this.turnDirectTriggerOn = true;
        }
    }

    private void turnDirectTriggerOff() {
        if (this.directTrigger) {
            this.directTrigger = false;
            this.turnDirectTriggerOff = true;
        }
        this.distanceSortIndexes = null;
    }

    public boolean getAndFlushTurnGFNITriggerOff() {
        var result = this.turnGFNITriggerOff;
        this.turnGFNITriggerOff = false;
        return result;
    }

    public boolean getAndFlushTurnDirectTriggerOn() {
        var result = this.turnDirectTriggerOn;
        this.turnDirectTriggerOn = false;
        return result;
    }

    public boolean getAndFlushTurnDirectTriggerOff() {
        var result = this.turnDirectTriggerOff;
        this.turnDirectTriggerOff = false;
        return result;
    }

    public double getDirectTriggerKey() {
        return this.directTriggerKey;
    }

    public void setDirectTriggerKey(double key) {
        this.directTriggerKey = key;
    }

    @Override
    public void prepareTrigger(boolean isAngleTrigger) {
        this.pendingTriggerIsAngle = isAngleTrigger;
    }

    @Override
    public void sortOnTrigger(Vector3fc cameraPos) {
        this.sort(cameraPos, this.pendingTriggerIsAngle, false);
    }

    private static int getAttemptsForTime(long ns) {
        return ns <= MAX_TOPO_SORT_PATIENT_TIME_NS ? PATIENT_TOPO_ATTEMPTS : REGULAR_TOPO_ATTEMPTS;
    }

    private void sort(Vector3fc cameraPos, boolean isAngleTrigger, boolean initial) {
        // mark as not being reused to ensure the updated buffer is actually uploaded
        this.unsetReuseUploadedData();

        // uses a topo sort or a distance sort depending on what is enabled
        IntBuffer indexBuffer = this.getBuffer().getDirectBuffer().asIntBuffer();

        if (this.quads.length > MAX_TOPO_SORT_QUADS) {
            this.turnGFNITriggerOff();
            this.turnDirectTriggerOn();
        }

        if (this.GFNITrigger && !isAngleTrigger) {
            var sortStart = initial ? 0 : System.nanoTime();

            var result = TopoGraphSorting.topoSortDepthFirstCyclic(
                    indexBuffer, this.quads, this.distancesByNormal, cameraPos);

            var sortTime = initial ? 0 : System.nanoTime() - sortStart;

            // if we've already failed, there's reduced patience for sorting since the
            // probability of failure and wasted compute time is higher. Initial sorting is
            // often very slow when the cpu is loaded and the JIT isn't ready yet, so it's
            // ignored here.
            if (!initial && sortTime > (this.consecutiveTopoSortFailures > 0
                    ? MAX_FAILING_TOPO_SORT_TIME_NS
                    : MAX_TOPO_SORT_TIME_NS)) {
                this.turnGFNITriggerOff();
                this.turnDirectTriggerOn();
                System.out.println("topo sort took too long");
            } else if (result) {
                // disable distance sorting because topo sort seems to be possible.
                this.turnDirectTriggerOff();
                this.consecutiveTopoSortFailures = 0;
                return;
            } else {
                // topo sort failure, the topo sort algorithm doesn't work on all cases

                // gives up after a certain number of failures. it keeps GFNI triggering with
                // topo sort on while the angle triggering is also active to maybe get a topo
                // sort success from a different angle.
                this.consecutiveTopoSortFailures++;
                if (this.consecutiveTopoSortFailures >= getAttemptsForTime(sortTime)) {
                    this.turnGFNITriggerOff();
                }
                this.turnDirectTriggerOn();
            }
        }

        if (this.directTrigger) {
            indexBuffer.rewind();
            this.distanceSortIndexes = distanceSortDirect(
                    this.distanceSortIndexes, indexBuffer, this.quads, cameraPos);
            return;
        }
    }

    private static ThreadLocal<float[]> distanceSortKeys = new ThreadLocal<>();

    // TODO: encode the quad key into the lower half of a long and the distance into
    // the upper half. since it's all positive distances, it should be able to just
    // sort the longs by value using Arrays.sort (or fastutil's sort) which is very
    // fast. In order to re-use the sort order the sorted array would need to be
    // iterated, each quad index extracted and then the distance computed and
    // written back into the upper part of the long. This uses more memory to keep
    // around the sort result, but requires less repeated iteration of the data.
    private static int[] distanceSortDirect(int[] indexes,
            IntBuffer indexBuffer, TQuad[] quads, Vector3fc cameraPos) {
        if (indexes == null) {
            indexes = new int[quads.length];
            for (int i = 0; i < quads.length; i++) {
                indexes[i] = i;
            }
        }

        float[] keys = distanceSortKeys.get();
        if (keys == null || keys.length < quads.length) {
            keys = new float[quads.length];
            distanceSortKeys.set(keys);
        }

        for (int i = 0; i < quads.length; i++) {
            keys[i] = cameraPos.distanceSquared(quads[i].center());
        }

        MergeSort.mergeSort(indexes, keys);
        TranslucentData.writeQuadVertexIndexes(indexBuffer, indexes);

        return indexes;
    }

    public static TopoSortDynamicData fromMesh(BuiltSectionMeshParts translucentMesh,
            Vector3fc cameraPos, TQuad[] quads, ChunkSectionPos sectionPos, TranslucentGeometryCollector collector,
            NativeBuffer buffer) {
        // prepare accumulation groups for GFNI integration and copy
        var size = Integer.bitCount(collector.getAlignedNormalBitmap()) + collector.getUnalignedDistanceCount();

        var distancesByNormal = new Object2ReferenceOpenHashMap<Vector3fc, float[]>(size);
        if (collector.getAlignedDistances() != null) {
            for (var accGroup : collector.getAlignedDistances()) {
                if (accGroup != null) {
                    accGroup.prepareAndInsert(distancesByNormal);
                }
            }
        }
        if (collector.getUnalignedDistanceCount() > 0) {
            for (var accGroup : collector.getUnalignedDistances()) {
                accGroup.prepareAndInsert(distancesByNormal);
            }
        }

        VertexRange range = TranslucentData.getUnassignedVertexRange(translucentMesh);
        if (buffer == null) {
            buffer = PresentTranslucentData.nativeBufferForQuads(quads);
        }

        var dynamicData = new TopoSortDynamicData(sectionPos, buffer, range, quads, collector, distancesByNormal);

        dynamicData.sort(cameraPos, false, true);

        return dynamicData;
    }

}
