package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import com.lodborg.intervaltree.DoubleInterval;

import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * A group represents a set of face planes of the same normal within a section.
 * A
 * group keeps a tree of its face plane distances to determine if it needs to be
 * triggered.
 */
class Group {
    /**
     * The section this group is for
     */
    ChunkSectionPos sectionPos;

    /**
     * A sorted list of all the face plane distances in this group.
     */
    double[] facePlaneDistances;

    /**
     * A hash of all the face plane distances in this group (before adding the base
     * distance)
     */
    long relDistanceHash = 0;

    /**
     * The closed (inclusive of both boundaries) minimum and maximum distances.
     */
    DoubleInterval distances;

    Group(AccumulationGroup accGroup, DoubleInterval accGroupDistances, double baseDistance) {
        replaceWith(accGroup, accGroupDistances, baseDistance);
    }

    void replaceWith(AccumulationGroup accGroup, DoubleInterval accGroupDistances, double baseDistance) {
        this.sectionPos = accGroup.sectionPos;
        this.distances = accGroupDistances;
        this.relDistanceHash = accGroup.relDistanceHash;

        // store the absolute face plane distances in an array
        this.facePlaneDistances = new double[accGroup.relativeDistances.size()];
        int i = 0;
        for (double relDistance : accGroup.relativeDistances) {
            this.facePlaneDistances[i++] = relDistance + baseDistance;
        }

        // sort the array ascending
        DoubleArrays.quickSort(facePlaneDistances);
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

    void triggerRange(GFNI gfni, double start, double end, int collectorKey) {
        // trigger self on the section if the query range overlaps with the group
        // testing for strict inequality because if the two intervals just touch at the
        // start/end, there can be no overlap
        if (start < this.distances.getEnd() && end > this.distances.getStart()
                && queryRange(this.facePlaneDistances, start, end)) {
            gfni.triggerSection(this.sectionPos, collectorKey);
        }
    }
}
