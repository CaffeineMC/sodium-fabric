package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import org.joml.Vector3fc;

import com.lodborg.intervaltree.DoubleInterval;
import com.lodborg.intervaltree.Interval.Bounded;

import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet;
import net.minecraft.util.math.ChunkSectionPos;

class AccumulationGroup {
    final DoubleOpenHashSet relativeDistances = new DoubleOpenHashSet(16);
    final Vector3fc normal;
    final int collectorKey;
    final ChunkSectionPos sectionPos;

    double[] facePlaneDistances; // relative to the base distance
    DoubleInterval distances;
    long relDistanceHash;
    double baseDistance;

    AccumulationGroup(ChunkSectionPos sectionPos, Vector3fc normal, int collectorKey) {
        this.sectionPos = sectionPos;
        this.normal = normal;
        this.collectorKey = collectorKey;
    }

    boolean addPlaneMember(float vertexX, float vertexY, float vertexZ) {
        double distance = this.normal.dot(vertexX, vertexY, vertexZ);

        // add the distance to the set and update the min/max distances if necessary
        return this.relativeDistances.add(distance);
    }

    void prepareIntegration() {
        // stop if already prepared
        if (this.facePlaneDistances != null) {
            throw new IllegalStateException("Already prepared");
        }

        // store the absolute face plane distances in an array
        var size = this.relativeDistances.size();
        this.facePlaneDistances = new double[this.relativeDistances.size()];
        int i = 0;
        for (double relDistance : this.relativeDistances) {
            this.facePlaneDistances[i++] = relDistance;

            long distanceBits = Double.doubleToLongBits(relDistance);
            this.relDistanceHash ^= distanceBits;
        }

        // sort the array ascending
        DoubleArrays.quickSort(facePlaneDistances);

        this.baseDistance = this.normal.dot(
                sectionPos.getMinX(), sectionPos.getMinY(), sectionPos.getMinZ());
        this.distances = new DoubleInterval(
                this.facePlaneDistances[0] + this.baseDistance,
                this.facePlaneDistances[size - 1] + this.baseDistance,
                Bounded.CLOSED);

    }
}
