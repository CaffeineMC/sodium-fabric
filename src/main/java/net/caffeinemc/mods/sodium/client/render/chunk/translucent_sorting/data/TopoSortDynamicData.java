package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.function.IntConsumer;

import org.joml.Vector3dc;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.caffeinemc.mods.sodium.client.gl.util.VertexRange;
import net.caffeinemc.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TQuad;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger.GeometryPlanes;
import net.caffeinemc.mods.sodium.client.util.NativeBuffer;
import net.caffeinemc.mods.sodium.client.util.sorting.RadixSort;
import net.minecraft.core.SectionPos;

/**
 * Performs dynamic topo sorting and falls back to distance sorting as
 * necessary. This class implements a number of heuristics to attempt to upgrade
 * distance-based sorting back to topo sorting when possible as topo sorting
 * generally needs to happen far less often.
 * 
 * Triggering is performed when the quads' planes crossed along their normal
 * direction (unidirectional).
 * 
 * Implementation note:
 * - Reusing the output of previous distance sorting job doesn't make a
 * difference or makes things slower in some cases. It's unclear why exactly
 * this happens, I suspect weird memory behavior or the reuse is not actually
 * that helpful to the sorting algorithm.
 */
public class TopoSortDynamicData extends DynamicData implements IntConsumer {
    private final TQuad[] quads;
    private boolean GFNITrigger = true;
    private boolean directTrigger = false;
    private boolean turnGFNITriggerOff = false;
    private boolean turnDirectTriggerOn = false;
    private boolean turnDirectTriggerOff = false;
    private double directTriggerKey = -1;
    private int consecutiveTopoSortFailures = 0;
    private boolean pendingTriggerIsDirect;
    private final Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal;
    private IntBuffer intBuffer;

    private static final int MAX_TOPO_SORT_QUADS = 1000;
    private static final int MAX_TOPO_SORT_TIME_NS = 1_000_000;
    private static final int MAX_FAILING_TOPO_SORT_TIME_NS = 750_000;
    private static final int MAX_TOPO_SORT_PATIENT_TIME_NS = 250_000;
    private static final int PATIENT_TOPO_ATTEMPTS = 5;
    private static final int REGULAR_TOPO_ATTEMPTS = 2;

    private TopoSortDynamicData(SectionPos sectionPos,
            NativeBuffer buffer, VertexRange range, TQuad[] quads,
            GeometryPlanes geometryPlanes, Vector3dc cameraPos,
            Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal) {
        super(sectionPos, buffer, range, geometryPlanes, cameraPos);
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
    public void prepareTrigger(boolean isDirectTrigger) {
        this.pendingTriggerIsDirect = isDirectTrigger;
    }

    @Override
    public void sortOnTrigger(Vector3fc cameraPos) {
        this.sort(cameraPos, this.pendingTriggerIsDirect, false);
    }

    private static int getAttemptsForTime(long ns) {
        return ns <= MAX_TOPO_SORT_PATIENT_TIME_NS ? PATIENT_TOPO_ATTEMPTS : REGULAR_TOPO_ATTEMPTS;
    }

    private void sort(Vector3fc cameraPos, boolean isDirectTrigger, boolean initial) {
        // mark as not being reused to ensure the updated buffer is actually uploaded
        this.unsetReuseUploadedData();

        // uses a topo sort or a distance sort depending on what is enabled
        IntBuffer indexBuffer = this.getBuffer().getDirectBuffer().asIntBuffer();

        if (this.quads.length > MAX_TOPO_SORT_QUADS) {
            this.turnGFNITriggerOff();
            this.turnDirectTriggerOn();
        }

        if (this.GFNITrigger && !isDirectTrigger) {
            var sortStart = initial ? 0 : System.nanoTime();

            this.intBuffer = indexBuffer;
            var result = TopoGraphSorting.topoGraphSort(this, this.quads, this.distancesByNormal, cameraPos);
            this.intBuffer = null;

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
            distanceSortDirect(indexBuffer, this.quads, cameraPos);
        }
    }

    /**
     * Sorts the given quads by descending center distance to the camera and writes
     * the resulting order to the given index buffer.
     */
    private static void distanceSortDirect(IntBuffer indexBuffer, TQuad[] quads, Vector3fc cameraPos) {
        if (quads.length <= 1) {
            TranslucentData.writeQuadVertexIndexes(indexBuffer, 0);
        } else if (RadixSort.useRadixSort(quads.length)) {
            final var keys = new int[quads.length];

            for (int q = 0; q < quads.length; q++) {
                keys[q] = ~Float.floatToRawIntBits(quads[q].getCenter().distanceSquared(cameraPos));
            }

            var indices = RadixSort.sort(keys);

            for (int i = 0; i < quads.length; i++) {
                TranslucentData.writeQuadVertexIndexes(indexBuffer, indices[i]);
            }
        } else {
            final var data = new long[quads.length];
            for (int q = 0; q < quads.length; q++) {
                float distance = quads[q].getCenter().distanceSquared(cameraPos);
                data[q] = (long) ~Float.floatToRawIntBits(distance) << 32 | q;
            }

            Arrays.sort(data);

            for (int i = 0; i < quads.length; i++) {
                TranslucentData.writeQuadVertexIndexes(indexBuffer, (int) data[i]);
            }
        }
    }

    public static TopoSortDynamicData fromMesh(BuiltSectionMeshParts translucentMesh,
            CombinedCameraPos cameraPos, TQuad[] quads, SectionPos sectionPos,
            GeometryPlanes geometryPlanes,
            NativeBuffer buffer) {
        var distancesByNormal = geometryPlanes.prepareAndGetDistances();

        VertexRange range = TranslucentData.getUnassignedVertexRange(translucentMesh);
        if (buffer == null) {
            buffer = PresentTranslucentData.nativeBufferForQuads(quads);
        }

        var dynamicData = new TopoSortDynamicData(sectionPos, buffer, range, quads, geometryPlanes,
                cameraPos.getAbsoluteCameraPos(), distancesByNormal);

        dynamicData.sort(cameraPos.getRelativeCameraPos(), false, true);

        return dynamicData;
    }

    @Override
    public void accept(int value) {
        TranslucentData.writeQuadVertexIndexes(this.intBuffer, value);
    }
}
