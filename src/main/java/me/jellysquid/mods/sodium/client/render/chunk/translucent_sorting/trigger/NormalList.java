package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import java.util.Collection;

import org.joml.Vector3dc;

import com.lodborg.intervaltree.DoubleInterval;
import com.lodborg.intervaltree.Interval;
import com.lodborg.intervaltree.Interval.Bounded;
import com.lodborg.intervaltree.IntervalTree;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.AlignableNormal;

/**
 * A normal list contains all the face planes that have the same normal.
 */
public class NormalList {
    /**
     * Size threshold after which group sets in {@link #groupsByInterval} are
     * replaced with hash sets to improve update performance.
     */
    private static final int HASH_SET_THRESHOLD = 20;

    /**
     * Size threshold under which group sets in {@link #groupsByInterval} are
     * downgraded to array sets to reduce memory usage.
     */
    private static final int ARRAY_SET_THRESHOLD = 10;

    /**
     * The normal of this normal list.
     */
    private final AlignableNormal normal;

    /**
     * An interval tree of group intervals. Since this only stores intervals, the
     * stored intervals are mapped to groups in a separate hashmap.
     */
    private final IntervalTree<Double> intervalTree = new IntervalTree<>();

    /**
     * A separate hashmap of groups. This is what actually stores the groups since
     * the interval tree just contains intervals.
     */
    private final Object2ReferenceOpenHashMap<DoubleInterval, Collection<Group>> groupsByInterval = new Object2ReferenceOpenHashMap<>();

    /**
     * A hashmap from chunk sections to groups. This is for finding groups during
     * updates.
     */
    private final Long2ReferenceOpenHashMap<Group> groupsBySection = new Long2ReferenceOpenHashMap<>();

    /**
     * Constructs a new normal list with the given unit normal vector and aligned
     * normal index.
     * 
     * @param normal       The unit normal vector
     * @param collectorKey The geometry collector index
     */
    NormalList(AlignableNormal normal) {
        this.normal = normal;
    }

    public AlignableNormal getNormal() {
        return this.normal;
    }

    private double normalDotDouble(Vector3dc v) {
        return Math.fma((double) this.normal.x, v.x(),
                Math.fma((double) this.normal.y, v.y(),
                        (double) this.normal.z * v.z()));
    }

    void processMovement(SortTriggering ts, CameraMovement movement) {
        // calculate the distance range of the movement with respect to the normal
        double start = this.normalDotDouble(movement.lastCamera());
        double end = this.normalDotDouble(movement.currentCamera());

        // stop if the movement is reverse with regards to the normal
        // since this means it's moving against the normal
        if (start >= end) {
            return;
        }

        // perform the interval query on the group intervals and resolve each interval
        // to the collection of groups it maps to
        var interval = new DoubleInterval(start, end, Bounded.CLOSED);
        for (Interval<Double> groupInterval : intervalTree.query(interval)) {
            for (Group group : groupsByInterval.get(groupInterval)) {
                group.triggerRange(ts, start, end);
            }
        }
    }

    private void removeGroupInterval(Group group) {
        var groups = this.groupsByInterval.get(group.distances);
        if (groups != null) {
            groups.remove(group);
            if (groups.isEmpty()) {
                this.groupsByInterval.remove(group.distances);

                // only remove from the interval tree if no other sections are also using it
                this.intervalTree.remove(group.distances);
            } else if (groups.size() <= ARRAY_SET_THRESHOLD) {
                groups = new ReferenceArraySet<>(groups);
                this.groupsByInterval.put(group.distances, groups);
            }
        }
    }

    private void addGroupInterval(Group group) {
        var groups = this.groupsByInterval.get(group.distances);
        if (groups == null) {
            groups = new ReferenceArraySet<>();
            this.groupsByInterval.put(group.distances, groups);

            // only add to the interval tree if it's a new interval
            this.intervalTree.add(group.distances);
        } else if (groups.size() >= HASH_SET_THRESHOLD) {
            groups = new ReferenceLinkedOpenHashSet<>(groups);
            this.groupsByInterval.put(group.distances, groups);
        }
        groups.add(group);
    }

    boolean hasSection(long sectionPos) {
        return this.groupsBySection.containsKey(sectionPos);
    }

    boolean isEmpty() {
        return this.groupsBySection.isEmpty();
    }

    void addSection(NormalPlanes normalPlanes, long sectionPos) {
        var group = new Group(normalPlanes);

        this.groupsBySection.put(sectionPos, group);
        this.addGroupInterval(group);
    }

    void removeSection(long sectionPos) {
        Group group = this.groupsBySection.remove(sectionPos);
        if (group != null) {
            this.removeGroupInterval(group);
        }
    }

    void updateSection(NormalPlanes normalPlanes, long sectionPos) {
        Group group = this.groupsBySection.get(sectionPos);

        // only update on changes to translucent geometry
        if (group.normalPlanesEquals(normalPlanes)) {
            // don't update if they are the same
            return;
        }

        this.removeGroupInterval(group);
        group.replaceWith(normalPlanes);
        this.addGroupInterval(group);
    }
}
