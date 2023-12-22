package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import org.joml.Vector3fc;
import java.util.Arrays;

import com.lodborg.intervaltree.DoubleInterval;
import com.lodborg.intervaltree.Interval.Bounded;

import it.unimi.dsi.fastutil.floats.FloatOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.AlignableNormal;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * NormalPlanes represents planes by a normal and a list of distances. Initially they're
 * stored in a hash set and later sorted for range queries.
 */
public class NormalPlanes {
    final FloatOpenHashSet relativeDistancesSet = new FloatOpenHashSet(16);
    final AlignableNormal normal;
    final ChunkSectionPos sectionPos;

    float[] relativeDistances; // relative to the base distance
    DoubleInterval distanceRange;
    long relDistanceHash;
    double baseDistance;

    private NormalPlanes(ChunkSectionPos sectionPos, AlignableNormal normal) {
        this.sectionPos = sectionPos;
        this.normal = normal;
    }

    public NormalPlanes(ChunkSectionPos sectionPos, Vector3fc normal) {
        this(sectionPos, AlignableNormal.fromUnaligned(normal));
    }

    public NormalPlanes(ChunkSectionPos sectionPos, int alignedDirection) {
        this(sectionPos, AlignableNormal.fromAligned(alignedDirection));
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
        Arrays.sort(relativeDistances);

        this.baseDistance = this.normal.dot(
                sectionPos.getMinX(), sectionPos.getMinY(), sectionPos.getMinZ());
        this.distanceRange = new DoubleInterval(
                this.relativeDistances[0] + this.baseDistance,
                this.relativeDistances[size - 1] + this.baseDistance,
                Bounded.CLOSED);
    }

    public void prepareAndInsert(Object2ReferenceOpenHashMap<Vector3fc, float[]> distancesByNormal) {
        this.prepareIntegration();
        if (distancesByNormal != null) {
            distancesByNormal.put(this.normal, this.relativeDistances);
        }
    }
}
