package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import it.unimi.dsi.fastutil.doubles.Double2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;

/**
 * A bucket contains all groups of a normal list that fall within a certain
 * distance range. The groups are non-overlapping but some sections may have two
 * groups to deal with sections that cross bucket borders. The bucket size is
 * chosen such that no section's faces can cover more than two buckets, even if
 * the normal is diagonal to the axes.
 * 
 * TODO: a more sophisticated structure instead of subbuckets that can
 * efficiently range query overlapping groups. Traditional segment trees use
 * integer segments so they aren't quite the right fit here.
 */
public class Bucket {
    private static final int SUBBUCKETS = 4;

    /**
     * The backing collection storing the groups in this bucket. An array of sets is
     * used to cut down on the number of groups that need to be iterated in range
     * queries.
     */
    @SuppressWarnings("unchecked")
    final ReferenceArraySet<Group>[] groups = new ReferenceArraySet[SUBBUCKETS];
    final Double2ReferenceAVLTreeMap<Group> allGroups = new Double2ReferenceAVLTreeMap<>();

    /**
     * The minimum and maximum distances of all the groups in this bucket.
     */
    private double minDistance;
    private double maxDistance;

    /**
     * The starting distance of this bucket. There don't need to be face planes
     * filling the whole bucket.
     * 
     * NOTE: groups may protrude outside the bucket and the min/max distances
     * reflect that. However, the normal list makes sure only groups that overlap
     * with this bucket are added.
     */
    final double startDistance;
    final double endDistance;

    Bucket(double startDistance, double bucketLength) {
        this.startDistance = startDistance;
        this.endDistance = startDistance + bucketLength;
    }

    private boolean isOutsideContentRange(double start, double end) {
        return start >= this.maxDistance || end <= this.minDistance;
    }

    private static int constrainIndex(int index) {
        return Math.max(0, Math.min(index, SUBBUCKETS - 1));
    }

    private int getSubbucketStart(double start) {
        return constrainIndex((int) Math.floor((start - this.startDistance) / SUBBUCKETS));
    }

    private int getSubbucketEnd(double end) {
        return getSubbucketStart(end) + 1;
    }

    double getStartBoundary() {
        if (isEmpty()) {
            return this.startDistance;
        }
        return Math.max(this.startDistance, this.minDistance);
    }

    double getEndBoundary() {
        if (isEmpty()) {
            return this.endDistance;
        }
        return Math.min(this.endDistance, this.maxDistance);
    }

    void triggerRange(GFNI gfni, double start, double end) {
        // stop if the query range is outside the bucket
        // (start of the query range is inclusive, end is exclusive) (?)
        if (isOutsideContentRange(start, end)) {
            return;
        }

        // iterate subbuckets that fall within the range or all of them if the whole
        // bucket is within the range
        // TODO: even better heuristic, it's more efficient to iterate all groups if the
        // the range is just _most_ of the whole bucket
        int subbucketStart = getSubbucketStart(start);
        int subbucketEnd = getSubbucketEnd(end);
        if (subbucketEnd - subbucketStart == SUBBUCKETS) {
            Group previousGroup = null;
            for (Group group : this.allGroups.values()) {
                // deduplicate groups since they're each entered twice
                if (group != previousGroup) {
                    group.triggerRange(gfni, start, end);
                }
                previousGroup = group;
            }
        } else {
            for (int i = subbucketStart; i < subbucketEnd; i++) {
                ReferenceArraySet<Group> subbucket = this.groups[i];
                if (subbucket != null) {
                    for (Group group : subbucket) {
                        group.triggerRange(gfni, start, end);
                    }
                }
            }
        }
    }

    void addNewGroup(Group group) {
        for (int i = getSubbucketStart(group.minDistance),
                endIndex = getSubbucketEnd(group.maxDistance); i < endIndex; i++) {
            ReferenceArraySet<Group> subbucket = this.groups[i];
            if (subbucket == null) {
                subbucket = new ReferenceArraySet<>();
                this.groups[i] = subbucket;
            }
            subbucket.add(group);
        }

        // if this is the first group being added, set the min/max distances directly
        if (this.allGroups.isEmpty()) {
            this.minDistance = group.minDistance;
            this.maxDistance = group.maxDistance;
        } else {
            this.minDistance = Math.min(this.minDistance, group.minDistance);
            this.maxDistance = Math.max(this.maxDistance, group.maxDistance);
        }
        this.allGroups.put(group.minDistance, group);
        this.allGroups.put(group.maxDistance, group);

    }

    void removeGroup(Group group) {
        for (int i = getSubbucketStart(group.minDistance),
                endIndex = getSubbucketEnd(group.maxDistance); i < endIndex; i++) {
            ReferenceArraySet<Group> subbucket = this.groups[i];
            if (subbucket != null) {
                subbucket.remove(group);

                if (subbucket.isEmpty()) {
                    this.groups[i] = null;
                }
            }
        }

        this.allGroups.remove(group.minDistance);
        this.allGroups.remove(group.maxDistance);
        if (!isEmpty()) {
            this.minDistance = this.allGroups.firstDoubleKey();
            this.maxDistance = this.allGroups.lastDoubleKey();
        }
    }

    void updateGroupRange(Group existingGroup, AccumulationGroup accGroup,
            double accGroupMinDistance, double accGroupMaxDistance) {
        // iterate the relevant subbuckets and add or remove the group from them
        // the existing group contains the old range, the accGroup contains the new
        int existingStart = getSubbucketStart(existingGroup.minDistance);
        int existingEnd = getSubbucketEnd(existingGroup.maxDistance);
        int newStart = getSubbucketStart(accGroupMinDistance);
        int newEnd = getSubbucketEnd(accGroupMaxDistance);
        for (int i = Math.min(existingStart, newStart), endIndex = Math.max(existingEnd, newEnd); i < endIndex; i++) {
            ReferenceArraySet<Group> subbucket = this.groups[i];
            boolean inExisting = i >= existingStart && i < existingEnd;
            boolean inNew = i >= newStart && i < newEnd;

            if (inNew && !inExisting) {
                if (subbucket == null) {
                    subbucket = new ReferenceArraySet<>();
                    this.groups[i] = subbucket;
                }
                subbucket.add(existingGroup);
            } else if (inExisting && !inNew) {
                if (subbucket == null) {
                    int a = 4; // TODO: remove
                }
                subbucket.remove(existingGroup);

                if (subbucket.isEmpty()) {
                    this.groups[i] = null;
                }
            }
        }

        if (accGroupMinDistance != existingGroup.minDistance) {
            this.allGroups.remove(existingGroup.minDistance);
            this.allGroups.put(accGroupMinDistance, existingGroup);
            this.minDistance = this.allGroups.firstDoubleKey();
        }
        if (accGroupMaxDistance != existingGroup.maxDistance) {
            this.allGroups.remove(existingGroup.maxDistance);
            this.allGroups.put(accGroupMaxDistance, existingGroup);
            this.maxDistance = this.allGroups.lastDoubleKey();
        }
    }

    boolean isEmpty() {
        return this.allGroups.isEmpty();
    }
}
