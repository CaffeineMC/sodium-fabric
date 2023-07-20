package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.doubles.Double2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * A normal list contains all the face planes that have the same normal.
 * 
 * TODO: instead of buckets, use an interval tree
 * https://github.com/lodborg/interval-tree for each normal list with the groups
 * in it. Since the interval tree is just a set of intervals, use a multi hash
 * map to map the intervals to the groups. There can be multiple groups per
 * interval.
 */
public class NormalList {
    /**
     * The normal of this normal list.
     */
    final Vector3dc normal;

    /**
     * The bucket size in terms of normal-relative distance, chosen such that no
     * section can span more than two buckets. This is effectively length of a
     * 16*16*16 chunk section in terms of normal-relative distance.
     */
    final float bucketLength;

    /**
     * map of bucket ranges to buckets, each bucket appears twice in this map. Since
     * the bucket ranges are disjoint, this allows us to find the bucket for a given
     * range even if the range is fully contained within a bucket. There can only
     * ever be a single bucket at any key.
     */
    final Double2ReferenceAVLTreeMap<Bucket> bucketRanges = new Double2ReferenceAVLTreeMap<>();

    /**
     * A separate hashmap of groups to enable fast retrieval of existing groups.
     */
    final Object2ReferenceOpenHashMap<ChunkSectionPos, Group> groups = new Object2ReferenceOpenHashMap<>();

    /**
     * Constructs a new normal list with the given unit normal vector.
     * 
     * @param normal The unit normal vector
     */
    NormalList(Vector3fc normal) {
        this.normal = new Vector3d(normal);
        this.bucketLength = Math.abs(normal.x() * 16) + Math.abs(normal.y() * 16) + Math.abs(normal.z() * 16);
    }

    /**
     * Picks a bucket size for the given normal. The size must be chosen such that
     * no section (16x16x16 cube) can span more than two buckets with respect to the
     * normal. For axis-aligned normals, the size is 16. For diagonal normals with
     * one axis-aligned component, the size is sqrt(16^2 + 16^2) ≈ 22.628 (rounded
     * up). For fully diagonal normals the size is sqrt(16^2 + 16^2 + 16^2) ≈ 27.713
     * (rounded up).
     * 
     * @param normal The unit normal vector
     * @return The bucket size
     */
    private static float getBucketSize(Vector3fc normal) {
        int zeroComponents = 0;
        if (normal.x() == 0) {
            zeroComponents++;
        }
        if (normal.y() == 0) {
            zeroComponents++;
        }
        if (normal.z() == 0) {
            zeroComponents++;
        }
        if (zeroComponents == 3) {
            throw new IllegalArgumentException("normal must not be zero");
        } else if (zeroComponents == 2) {
            return 16f;
        } else if (zeroComponents == 1) {
            return 22.628f;
        } else {
            return 27.713f;
        }
    }

    /**
     * Finds the 0-2 buckets that fall within the given range. However the range may
     * be smaller than a bucket, in which case the enclosing bucket is returned.
     * 
     * @param start The start of the range
     * @param end   The end of the range
     * @return The buckets that fall within the range
     */
    private Collection<Bucket> queryBuckets(double start, double end) {
        if (start > end) {
            throw new IllegalArgumentException("start must be less than or equal to end");
        }

        // do a simple range query to see which buckets fall within the range
        // if there are any, simply return them as there can't be any buckets enclosing
        // the range if there are bucket boundaries within the range
        var innerBuckets = this.bucketRanges.subMap(start, end);
        if (!innerBuckets.isEmpty()) {
            // each bucket can appear twice in the map, so remove duplicates
            if (innerBuckets.size() == 1) {
                return innerBuckets.values();
            } else {
                Bucket first = innerBuckets.get(innerBuckets.firstDoubleKey());
                Bucket last = innerBuckets.get(innerBuckets.lastDoubleKey());
                if (first == last) {
                    return Collections.singleton(first);
                } else {
                    return List.of(first, last);
                }
            }
        }

        // in this case the range is enclosed by a single bucket, or none
        // there is an enclosing bucket if the two next bucket boundaries are the same
        // bucket
        // otherwise there is no enclosing bucket
        var headMap = this.bucketRanges.headMap(start);
        if (headMap.isEmpty()) {
            return Collections.emptyList();
        }
        var tailMap = this.bucketRanges.tailMap(end);
        if (tailMap.isEmpty()) {
            return Collections.emptyList();
        }

        var headEntry = headMap.get(headMap.lastDoubleKey());
        var tailEntry = tailMap.get(tailMap.firstDoubleKey());
        if (headEntry == tailEntry) {
            return Collections.singleton(headEntry);
        } else {
            return Collections.emptyList();
        }
    }

    void processMovement(GFNI gfni, double lastCameraX, double lastCameraY, double lastCameraZ,
            double cameraX, double cameraY, double cameraZ) {
        // calculate the distance range of the movement with respect to the normal
        double start = this.normal.dot(lastCameraX, lastCameraY, lastCameraZ);
        double end = this.normal.dot(cameraX, cameraY, cameraZ);
        // double start = -100000;
        // double end = 100000;

        // stop if the movement is reverse with regards to the normal
        // this means it's moving against the normal
        if (start > end) {
            return;
        }

        // range query for triggering the 0-2 buckets that fall within the range
        var queryBuckets = queryBuckets(start, end);
        if (!queryBuckets.isEmpty()) {
            for (Bucket bucket : queryBuckets) {
                bucket.triggerRange(gfni, start, end);
            }
        }
    }

    private Bucket createBucketAt(double startDistance) {
        return new Bucket(startDistance, this.bucketLength);
    }

    void updateGroup(AccumulationGroup accGroup) {
        // find the existing buckets that the group could be in
        ChunkSectionPos pos = accGroup.sectionPos;
        double baseDistance = accGroup.normal.dot(pos.getMinX(), pos.getMinY(), pos.getMinZ());
        double accGroupMinDistance = accGroup.getMinDistance(baseDistance);
        double accGroupMaxDistance = accGroup.getMaxDistance(baseDistance);

        Group group = this.groups.get(pos);
        boolean groupIsNew = false;
        if (group == null) {
            // stop on new irrelevant groups
            if (!accGroup.isRelevant()) {
                return;
            }

            // make a new group for this accumulation group
            group = new Group(accGroup, accGroupMinDistance, accGroupMaxDistance, baseDistance);
            groupIsNew = true;
            this.groups.put(pos, group);
        } else {
            // this is just a heuristic, but it assumes that if the size, bounds and
            // hash are equal, they are most likely the same. We also know that the existing
            // and new data is for the same section position since the group was retrieved
            // from the map for the right position.
            // TODO: how common are collisions and are they bad?
            // If they are common, use second hash
            if (group.facePlaneDistances.size() == accGroup.relativeDistances.size()
                    && group.minDistance == accGroupMinDistance
                    && group.maxDistance == accGroupMaxDistance
                    && group.relDistanceHash == accGroup.relDistanceHash) {
                // don't update if they are the same
                return;
            }
        }

        // determine the buckets in which the existing group is
        var existingBuckets = queryBuckets(group.minDistance, group.maxDistance);

        // determine the buckets in which the new group will be
        Collection<Bucket> newBuckets;
        if (accGroup.isRelevant()) {
            // find the new buckets for this group and create new ones if necessary.
            // determine how many buckets this group will be in
            double groupFirstBucketIndex = Math.floor(accGroupMinDistance / this.bucketLength);
            double groupSecondBucketIndex = Math.floor(accGroupMaxDistance / this.bucketLength);
            double groupFirstStartDistance = groupFirstBucketIndex * this.bucketLength;
            double groupSecondStartDistance = groupSecondBucketIndex * this.bucketLength;
            double groupSecondEndDistance = groupSecondStartDistance + this.bucketLength;
            int neededBuckets = (int) (groupSecondBucketIndex - groupFirstBucketIndex + 1);

            newBuckets = queryBuckets(groupFirstStartDistance, groupSecondEndDistance);

            int newBucketCount = newBuckets.size();
            if (newBucketCount < neededBuckets) {
                // create the missing buckets
                if (neededBuckets == 1) {
                    // one bucket, groupStartBucketIndex == groupEndBucketIndex
                    newBuckets = Collections.singleton(createBucketAt(groupFirstStartDistance));
                } else if (newBucketCount == 0) {
                    // create two new buckets, one for the start and one for the end
                    var startBucket = createBucketAt(groupFirstStartDistance);
                    var endBucket = createBucketAt(groupSecondStartDistance);
                    newBuckets = List.of(startBucket, endBucket);
                } else {
                    // one of the buckets is missing, find out which one and create it
                    // if only one bucket is needed but none were found, then the same process is
                    // ok, since groupSecondStartDistance == groupFirstStartDistance
                    var existingBucket = newBuckets.iterator().next();
                    var missingBucketStartDistance = existingBucket.startDistance == groupFirstStartDistance
                            ? groupSecondStartDistance
                            : groupFirstStartDistance;
                    newBuckets = Collections.singleton(createBucketAt(missingBucketStartDistance));
                }
            }
        } else {
            // use an empty set of new buckets to remove the group from all buckets
            // if it is irrelevant (meaning it has none or only one face plane distance)
            newBuckets = Collections.emptySet();
        }

        // go through the collected buckets (of which there can be at most two)
        // and process additions/removals of the group in both buckets
        var allBuckets = new ReferenceArraySet<Bucket>(2);
        allBuckets.addAll(existingBuckets);
        allBuckets.addAll(newBuckets);
        for (Bucket bucket : allBuckets) {
            var bucketPrevStartBoundary = bucket.getStartBoundary();
            var bucketPrevEndBoundary = bucket.getEndBoundary();

            boolean inExisting = existingBuckets.contains(bucket);
            if (newBuckets.contains(bucket)) {
                if (inExisting) {
                    bucket.updateGroupRange(group, accGroup, accGroupMinDistance, accGroupMaxDistance);
                } else {
                    bucket.addNewGroup(group);
                }
            } else if (inExisting) {
                bucket.removeGroup(group);

                // remove buckets using their previous distance range because the bucket has
                // already updates its own boundaries (but those aren't the ones used in the
                // tree)
                if (bucket.isEmpty()) {
                    this.bucketRanges.remove(bucketPrevStartBoundary);
                    this.bucketRanges.remove(bucketPrevEndBoundary);

                    // prevent re-adding the bucket when its range is updated in the tree later
                    continue;
                }
            } else {
                throw new RuntimeException("bucketChange called but group is neither in existing nor in new bucket");
            }

            // since bucket min/max distances can extend beyond the bucket boundaries
            // make sure to limit this using their actual boundaries.
            // this ensures the invariant that buckets don't overlap is maintained.
            double bucketNewStartBoundary = bucket.getStartBoundary();
            if (bucketNewStartBoundary != bucketPrevStartBoundary) {
                this.bucketRanges.remove(bucketPrevStartBoundary);
                this.bucketRanges.put(bucketNewStartBoundary, bucket);
            }
            double bucketNewEndBoundary = bucket.getEndBoundary();
            if (bucketNewEndBoundary != bucketPrevEndBoundary) {
                this.bucketRanges.remove(bucketPrevEndBoundary);
                this.bucketRanges.put(bucketNewEndBoundary, bucket);
            }
        }

        // if the group is new, then it won't be removed from all buckets
        // it also doesn't need to be replaced with updated data
        if (!groupIsNew) {
            // entirely remove the group if it has been removed from all buckets
            // this means it no longer needs to trigger at all
            if (newBuckets.isEmpty()) {
                this.groups.remove(pos);
            } else {
                // replace the group with the new data from accGroup
                // up until this point the group is unchanged
                group.replaceWith(accGroup, accGroupMinDistance, accGroupMaxDistance, baseDistance);
            }
        }
    }

    boolean isEmpty() {
        return this.groups.isEmpty();
    }
}
