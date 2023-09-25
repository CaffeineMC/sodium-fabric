package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import com.lodborg.intervaltree.DoubleInterval;

import it.unimi.dsi.fastutil.doubles.DoubleAVLTreeSet;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * A group represents a set of face planes of the same normal within a bucket. A
 * group keeps a tree of its face plane distances to determine if it needs to be
 * triggered.
 */
class Group {
    /**
     * The section this group is for
     */
    ChunkSectionPos sectionPos;

    /**
     * A tree of all the face plane distances in this group, relative to the
     * enclosing bucket's startDistance.
     */
    DoubleAVLTreeSet facePlaneDistances = new DoubleAVLTreeSet();

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

        // clear the existing tree and add all the face plane distances but add the base
        // distance to each relative distance
        this.facePlaneDistances.clear();
        for (double distance : accGroup.relativeDistances) {
            this.facePlaneDistances.add(distance + baseDistance);
        }
    }

    void triggerRange(GFNI gfni, double start, double end, int collectorKey) {
        // trigger self on the section if the query range overlaps with the group
        if (start < this.distances.getEnd() && end > this.distances.getStart()
                && !this.facePlaneDistances.subSet(start, end).isEmpty()) {
            gfni.triggerSection(this.sectionPos, collectorKey);
        }
    }
}
