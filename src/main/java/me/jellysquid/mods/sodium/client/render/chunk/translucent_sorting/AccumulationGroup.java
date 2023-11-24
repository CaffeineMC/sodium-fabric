package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting;

import org.joml.Vector3fc;

import com.lodborg.intervaltree.DoubleInterval;
import com.lodborg.intervaltree.Interval.Bounded;

import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.util.math.ChunkSectionPos;

public class AccumulationGroup {
    final FloatOpenHashSet relativeDistancesSet = new FloatOpenHashSet(16);
    final Vector3fc normal;
    final int collectorKey;
    final ChunkSectionPos sectionPos;

    float[] relativeDistances; // relative to the base distance
    DoubleInterval distanceRange;
    long relDistanceHash;
    double baseDistance;

    static int collectorKeyFromNormal(int normalX, int normalY, int normalZ) {
        return 0xFF | (normalX & 0xFF << 8) | (normalY & 0xFF << 15) | (normalZ & 0xFF << 22);
    }

    public AccumulationGroup(ChunkSectionPos sectionPos, Vector3fc normal, int collectorKey) {
        this.sectionPos = sectionPos;
        this.normal = normal;
        this.collectorKey = collectorKey;
    }

    boolean addPlaneMember(float vertexX, float vertexY, float vertexZ) {
        return this.addPlaneMember(this.normal.dot(vertexX, vertexY, vertexZ));
    }

    public boolean addPlaneMember(float distance) {
        return this.relativeDistancesSet.add(distance);
    }

    public void prepareIntegration() {
        // stop if already prepared
        if (this.relativeDistances != null) {
            throw new IllegalStateException("Already prepared");
        }

        // store the absolute face plane distances in an array
        var size = this.relativeDistancesSet.size();
        this.relativeDistances = new float[this.relativeDistancesSet.size()];
        int i = 0;
        for (float relDistance : this.relativeDistancesSet) {
            this.relativeDistances[i++] = relDistance;

            long distanceBits = Double.doubleToLongBits(relDistance);
            this.relDistanceHash ^= this.relDistanceHash * 31L + distanceBits;
        }

        // sort the array ascending
        FloatArrays.quickSort(relativeDistances);

        this.baseDistance = this.normal.dot(
                sectionPos.getMinX(), sectionPos.getMinY(), sectionPos.getMinZ());
        this.distanceRange = new DoubleInterval(
                this.relativeDistances[0] + this.baseDistance,
                this.relativeDistances[size - 1] + this.baseDistance,
                Bounded.CLOSED);

    }

    public void prepareAndInsert(Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal) {
        this.prepareIntegration();
        distancesByNormal.put(this.normal, this.relativeDistances);
    }
}
