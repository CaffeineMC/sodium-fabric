package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting;

import java.util.Collection;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

import com.lodborg.intervaltree.DoubleInterval;
import com.lodborg.intervaltree.Interval;
import com.lodborg.intervaltree.Interval.Bounded;
import com.lodborg.intervaltree.IntervalTree;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;

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
    private final Vector3dc normal;
    private final Vector3fc normalf;

    /**
     * If this normal list is for an axis-aligned normal, this is the index of it
     * for querying in
     * {@link TranslucentGeometryCollector#getGroupForNormal(Vector3fc)}.
     * 
     * If this normal list is for an unaligned normal, this is the key for the
     * hash map of quantized normals.
     */
    private final int collectorKey;

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
    NormalList(Vector3fc normal, int collectorKey) {
        this.normalf = normal;
        this.normal = new Vector3d(normal);
        this.collectorKey = collectorKey;
    }

    public Vector3fc getNormal() {
        return normalf;
    }

    int getCollectorKey() {
        return collectorKey;
    }

    void processMovement(TranslucentSorting ts, CameraMovement movement) {
        // calculate the distance range of the movement with respect to the normal
        double start = this.normal.dot(movement.lastCamera());
        double end = this.normal.dot(movement.currentCamera());

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
                group.triggerRange(ts, start, end, this.collectorKey);
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

    void addSection(AccumulationGroup accGroup, long sectionPos) {
        var group = new Group(accGroup);

        this.groupsBySection.put(sectionPos, group);
        this.addGroupInterval(group);
    }

    void removeSection(long sectionPos) {
        Group group = this.groupsBySection.remove(sectionPos);
        if (group != null) {
            this.removeGroupInterval(group);
        }
    }

    void updateSection(AccumulationGroup accGroup, long sectionPos) {
        Group group = this.groupsBySection.get(sectionPos);

        // only update on changes to translucent geometry
        if (group.equalsAccGroup(accGroup)) {
            // don't update if they are the same
            return;
        }

        this.removeGroupInterval(group);
        group.replaceWith(accGroup);
        this.addGroupInterval(group);
    }
}
