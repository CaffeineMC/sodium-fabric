package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet;
import net.minecraft.util.math.ChunkSectionPos;

public class AccumulationGroup {
    final DoubleOpenHashSet relativeDistances = new DoubleOpenHashSet(16);
    final Vector3fc normal;
    final ChunkSectionPos sectionPos;
    private double minDistance = Double.POSITIVE_INFINITY;
    private double maxDistance = Double.NEGATIVE_INFINITY;
    long relDistanceHash = 0;

    public AccumulationGroup(ChunkSectionPos sectionPos, Vector3fc normal) {
        this.sectionPos = sectionPos;
        this.normal = normal;
    }

    void add(float vertexX, float vertexY, float vertexZ) {
        add(this.normal.dot(vertexX, vertexY, vertexZ));
    }

    void add(double distance) {
        // add the distance to the set and update the min/max distances if necessary
        if (this.relativeDistances.add(distance)) {
            this.minDistance = Math.min(this.minDistance, distance);
            this.maxDistance = Math.max(this.maxDistance, distance);

            long distanceBits = Double.doubleToLongBits(distance);
            this.relDistanceHash ^= distanceBits;
        }
    }

    boolean isRelevant() {
        return relativeDistances.size() > 1;
    }

    double getMinDistance(double baseDistance) {
        return this.minDistance + baseDistance;
    }

    double getMaxDistance(double baseDistance) {
        return this.maxDistance + baseDistance;
    }
}
