package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.time.Duration;
import java.time.Instant;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Global Face Normal Indexing as described in
 * https://hackmd.io/@douira100/sodium-sl-gfni
 * 
 * Distances are stored as doubles and normals are stored as float vectors.
 * 
 * TODO: make Group and Bucket relative to use floats instead of doubles?
 * 
 * TODO: synchronization is used for integrating group builders submitted from
 * chunk build threads. It is assumed that integrating a group builder is fast
 * and thus synchronization is not a big problem. However, if it is, it might be
 * better to put the group builder in the ChunkBuildResult and have the main
 * thread integrate it. The distadvantage is that the main thread would be busy
 * with group integration while actually the chunk build thread could be doing
 * it itself. If fine grained synchronization is needed, threads could also work
 * on integrating individual normal lists in parallel. However, since normal
 * lists are created (and possibly deleted) on demand, this would require a more
 * complex synchronization scheme.
 * 
 * TODO: destory functionality similar to RenderSectionManger's destroy method?
 * 
 * TODO: does teleporting and switching dimensions work?
 */
public class GFNI {
    /**
     * A map of all the normal lists, indexed by their normal.
     */
    private Object2ReferenceOpenHashMap<Vector3fc, NormalList> normalLists = new Object2ReferenceOpenHashMap<>(50);

    /**
     * A temporary set of sections that were triggered during movement. A set is
     * needed because the same section can be triggered multiple times by different
     * normal lists.
     */
    ObjectOpenHashSet<ChunkSectionPos> triggeredSections = new ObjectOpenHashSet<>(50);

    public void processMovement(double lastCameraX, double lastCameraY, double lastCameraZ,
            double cameraX, double cameraY, double cameraZ) {
        var startTime = Instant.now();
        triggeredSections.clear();

        for (var normalList : this.normalLists.values()) {
            normalList.processMovement(this, lastCameraX, lastCameraY, lastCameraZ, cameraX, cameraY, cameraZ);
        }

        // TODO: do something with the triggered sections
        // for now just log the amount
        int triggerCount = this.triggeredSections.size();
        if (triggerCount > 0) {
            System.out.println("Triggered " + triggerCount + " sections");
            for (ChunkSectionPos section : this.triggeredSections) {
                System.out.println(section.getX() + " " + section.getY() + " " + section.getZ());
            }
        }

        // TODO: remove
        var duration = Duration.between(startTime, Instant.now());
        System.out.println("Processed movement in " + (float) duration.toNanos() / 1_000_000 + " ms");
    }

    void triggerSection(ChunkSectionPos section) {
        this.triggeredSections.add(section);
    }

    private void removeSectionFromList(NormalList normalList, long chunkSectionLongPos) {
        normalList.removeSection(chunkSectionLongPos);
        if (normalList.isEmpty()) {
            this.normalLists.remove(normalList.getNormal());
        }
    }

    public void removeSection(long chunkSectionLongPos) {
        var startTime = Instant.now();
        int normalListCount = this.normalLists.size();

        for (var normalList : this.normalLists.values()) {
            removeSectionFromList(normalList, chunkSectionLongPos);
        }

        // TODO: remove
        if (normalListCount != this.normalLists.size()) {
            System.out.println(normalLists.size() + " normal lists");
        }
        var duration = Duration.between(startTime, Instant.now());
        System.out.println("Processed section removal in " + (float) duration.toNanos() / 1_000_000 + " ms");

    }

    private void addSectionInNewNormalLists(AccumulationGroup accGroup) {
        var normal = accGroup.normal;
        var normalList = this.normalLists.get(normal);
        if (normalList == null) {
            normalList = new NormalList(normal, accGroup.groupBuilderKey);
            this.normalLists.put(normal, normalList);
            normalList.addSection(accGroup, accGroup.sectionPos.asLong());
        }
    }

    // TODO: synchronized because it's expected to be fast, see class comment
    public synchronized void integrateGroupBuilder(GroupBuilder builder) {
        long chunkSectionLongPos = builder.sectionPos.asLong();

        // if the builder is irrelevant, remove it from the normal lists
        if (!builder.isRelevant()) {
            removeSection(chunkSectionLongPos);
            return;
        }

        var startTime = Instant.now();

        int normalListCount = this.normalLists.size();

        // go through all normal lists and check against the normals that the group
        // builder has. if the normal list has data for the section, but the group
        // builder doesn't, the group is removed. otherwise, the group is updated.
        for (var normalList : this.normalLists.values()) {
            // check if the group builder includes data for this normal.
            var accGroup = builder.getGroupForNormal(normalList);
            if (normalList.hasSection(chunkSectionLongPos)) {
                if (accGroup == null) {
                    removeSectionFromList(normalList, chunkSectionLongPos);
                } else {
                    normalList.updateSection(accGroup, chunkSectionLongPos);
                }
            } else if (accGroup != null) {
                normalList.addSection(accGroup, chunkSectionLongPos);
            }
        }

        // go through the data of the group builder to check for data of new normals
        // for which there are no normal lists yet. This only checks for new normal
        // lists since new data for existing normal lists is handled above.
        if (builder.axisAlignedDistances != null) {
            for (var accGroup : builder.axisAlignedDistances) {
                if (accGroup != null) {
                    addSectionInNewNormalLists(accGroup);
                }
            }
        }
        if (builder.unalignedDistances != null) {
            for (var accGroup : builder.unalignedDistances.values()) {
                addSectionInNewNormalLists(accGroup);
            }
        }

        // TODO: remove
        if (normalListCount != this.normalLists.size()) {
            System.out.println(normalLists.size() + " normal lists");
        }
        var duration = Duration.between(startTime, Instant.now());
        System.out.println("Processed section add/update in " + (float) duration.toNanos() / 1_000_000 + " ms");
    }
}
