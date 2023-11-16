package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import com.lodborg.intervaltree.DoubleInterval;

import it.unimi.dsi.fastutil.doubles.DoubleArrays;

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
    double[] facePlaneDistances;

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

    Group(AccumulationGroup accGroup) {
        this.replaceWith(accGroup);
    }

    void replaceWith(AccumulationGroup accGroup) {
        this.sectionPos = accGroup.sectionPos.asLong();
        this.distances = accGroup.distances;
        this.relDistanceHash = accGroup.relDistanceHash;
        this.facePlaneDistances = accGroup.facePlaneDistances;
        this.baseDistance = accGroup.baseDistance;
    }

    public static boolean queryRange(double[] sortedDistances, double start, double end) {
        // test that there is actually an entry in the query range
        int result = DoubleArrays.binarySearch(sortedDistances, start);
        if (result < 0) {
            // recover the insertion point
            int insertionPoint = -result - 1;
            if (insertionPoint >= sortedDistances.length) {
                // no entry in the query range
                return false;
            }

            // check if the entry at the insertion point, which is the next one greater than
            // the start value, is less than or equal to the end value
            if (sortedDistances[insertionPoint] <= end) {
                // there is an entry in the query range
                return true;
            }
        } else {
            // exact match, trigger
            return true;
        }
        return false;
    }

    void triggerRange(TranslucentSorting ts, double start, double end, int collectorKey) {
        // trigger self on the section if the query range overlaps with the group
        // testing for strict inequality because if the two intervals just touch at the
        // start/end, there can be no overlap
        if (start < this.distances.getEnd() && end > this.distances.getStart()
                && queryRange(this.facePlaneDistances,
                        start - this.baseDistance, end - this.baseDistance)) {
            ts.triggerSectionGFNI(this.sectionPos, collectorKey);
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
     * If they are common, use second hash
     */
    boolean equalsAccGroup(AccumulationGroup accGroup) {
        return this.facePlaneDistances.length == accGroup.relativeDistances.size()
                && this.distances.equals(accGroup.distances)
                && this.relDistanceHash == accGroup.relDistanceHash;
    }
}
