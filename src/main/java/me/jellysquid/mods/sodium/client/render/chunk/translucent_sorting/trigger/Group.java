package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import com.lodborg.intervaltree.DoubleInterval;

import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.AlignableNormal;

/**
 * A group represents a set of face planes of the same normal within a section.
 *
 * group keeps a tree of its face plane distances to determine if it needs to be
 * triggered.
 */
class Group {
    /**
     * The section this group is for
     */
    long sectionPos;

    /**
     * A sorted list of all the face plane distances in this group. Relative to the
     * base distance.
     */
    float[] facePlaneDistances;

    /**
     * A hash of all the face plane distances in this group (before adding the base
     * distance)
     */
    long relDistanceHash;

    /**
     * The closed (inclusive of both boundaries) minimum and maximum distances.
     * Absolute values, not relative to the base distance.
     */
    DoubleInterval distances;

    double baseDistance;

    AlignableNormal normal;

    Group(NormalPlanes normalPlanes) {
        this.replaceWith(normalPlanes);
    }

    void replaceWith(NormalPlanes normalPlanes) {
        this.sectionPos = normalPlanes.sectionPos.asLong();
        this.distances = normalPlanes.distanceRange;
        this.relDistanceHash = normalPlanes.relDistanceHash;
        this.facePlaneDistances = normalPlanes.relativeDistances;
        this.baseDistance = normalPlanes.baseDistance;
        this.normal = normalPlanes.normal;
    }

    void triggerRange(SortTriggering ts, double start, double end) {
        // trigger self on the section if the query range overlaps with the group
        // testing for strict inequality because if the two intervals just touch at the
        // start/end, there can be no overlap
        if (start < this.distances.getEnd() && end > this.distances.getStart()
                && AlignableNormal.queryRange(this.facePlaneDistances,
                        (float) (start - this.baseDistance), (float) (end - this.baseDistance))) {
            ts.triggerSectionGFNI(this.sectionPos, this.normal);
        }
    }

    /**
     * A pretty good heuristic for equality of captured translucent geometry data.
     * 
     * It assumes that if the size, bounds, and hash are equal, they are most likely
     * the same. We also know that the existing and new data is for the same section
     * position since the group was retrieved from the map for the right position.
     * 
     * TODO: how common are collisions and are they bad?
     * If they are common, use second or different hash
     */
    boolean normalPlanesEquals(NormalPlanes normalPlanes) {
        return this.facePlaneDistances.length == normalPlanes.relativeDistancesSet.size()
                && this.distances.equals(normalPlanes.distanceRange)
                && this.relDistanceHash == normalPlanes.relDistanceHash;
    }
}
