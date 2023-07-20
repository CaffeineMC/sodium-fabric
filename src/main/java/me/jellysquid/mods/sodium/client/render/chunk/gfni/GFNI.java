package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Global Face Normal Indexing as described in
 * https://hackmd.io/@douira100/sodium-sl-gfni
 * 
 * Distances are stored as doubles and normals are stored as float vectors.
 * 
 * TODO: make Group and Bucket relative to use floats instead of doubles
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
 * TODO: handle unloading of sections, there are no updates when the section
 * just goes away. this needs to be hooked into and handled like an update to an
 * irrelevant new group.
 * 
 * TODO: for some reason in the first time using a void world, blocks added with
 * setblock are invisible.
 * 
 * TODO: does teleporting and switching dimensions work?
 */
public class GFNI {
    /**
     * A map of all the normal lists, indexed by their normal.
     */
    Object2ReferenceOpenHashMap<Vector3fc, NormalList> normalLists = new Object2ReferenceOpenHashMap<>(50);

    ReferenceOpenHashSet<ChunkSectionPos> triggeredSections = new ReferenceOpenHashSet<>(50);

    public void processMovement(double lastCameraX, double lastCameraY, double lastCameraZ,
            double cameraX, double cameraY, double cameraZ) {
        triggeredSections.clear();

        try {
            for (NormalList normalList : this.normalLists.values()) {
                normalList.processMovement(this, lastCameraX, lastCameraY, lastCameraZ, cameraX, cameraY, cameraZ);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
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
    }

    void triggerSection(ChunkSectionPos section) {
        this.triggeredSections.add(section);
    }

    // TODO: synchronized because it's expected to be fast, see class comment
    public synchronized void integrateGroupBuilder(GroupBuilder builder) {
        try {
            for (AccumulationGroup accGroup : builder.alignedFaceDistances) {
                if (accGroup != null) {
                    updateGroup(accGroup);
                }
            }
            for (AccumulationGroup accGroup : builder.unalignedDistances.values()) {
                updateGroup(accGroup);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void updateGroup(AccumulationGroup accGroup) {
        var normal = accGroup.normal;
        NormalList normalList = this.normalLists.get(normal);
        if (normalList == null) {
            // skip even creating a normal list if the group is irrelevant
            if (accGroup.isRelevant()) {
                normalList = new NormalList(normal);
                this.normalLists.put(normal, normalList);
                normalList.updateGroup(accGroup);
            }
        } else {
            // irrelevant groups do need to be updated on existing normal lists because
            // existing data has to be removed
            normalList.updateGroup(accGroup);

            if (normalList.isEmpty()) {
                this.normalLists.remove(normal);
            }
        }
    }
}
