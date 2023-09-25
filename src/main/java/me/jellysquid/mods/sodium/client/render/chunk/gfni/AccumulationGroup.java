package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import org.joml.Vector3fc;

import com.lodborg.intervaltree.DoubleInterval;
import com.lodborg.intervaltree.Interval.Bounded;

import it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet;
import net.minecraft.util.math.ChunkSectionPos;

class AccumulationGroup {
    final DoubleOpenHashSet relativeDistances = new DoubleOpenHashSet(16);
    final Vector3fc normal;
    final int collectorKey;
    final ChunkSectionPos sectionPos;
    long relDistanceHash = 0;

    private double relMinDistance = Double.POSITIVE_INFINITY;
    private double relMaxDistance = Double.NEGATIVE_INFINITY;

    AccumulationGroup(ChunkSectionPos sectionPos, Vector3fc normal, int collectorKey) {
        this.sectionPos = sectionPos;
        this.normal = normal;
        this.collectorKey = collectorKey;
    }

    boolean addPlaneMember(float vertexX, float vertexY, float vertexZ) {
        double distance = this.normal.dot(vertexX, vertexY, vertexZ);

        // add the distance to the set and update the min/max distances if necessary
        if (this.relativeDistances.add(distance)) {
            this.relMinDistance = Math.min(this.relMinDistance, distance);
            this.relMaxDistance = Math.max(this.relMaxDistance, distance);

            long distanceBits = Double.doubleToLongBits(distance);
            this.relDistanceHash ^= distanceBits;
            return true;
        }
        return false;
    }

    DoubleInterval getDistanceInterval(double baseDistance) {
        return new DoubleInterval(
                this.relMinDistance + baseDistance,
                this.relMaxDistance + baseDistance,
                Bounded.CLOSED);
    }
}
