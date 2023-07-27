package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.util.function.Consumer;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Global Face Normal Indexing as described in
 * https://hackmd.io/@douira100/sodium-sl-gfni
 * 
 * Distances are stored as doubles and normals are stored as float vectors.
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
 * Maximum add/update and remove durations are 0.4ms and 0.06ms respectively in
 * a 32rd world with around 230 normal lists.
 * 
 * It triggers 600 sections for normal (0, 1, 0) at distance
 * 62.88788890838623 in a 32rd world because of flooded caves that make the
 * ocean surface heuristic not work. The distance being triggered on is the
 * overworld water surface height. Flooded caves have water surfaces below solid
 * blocks above the water source blocks.
 * 
 * TODO:
 * - update GFNI document about how this implementation works with the
 * interval tree and update the diagram.
 * - filter the triggered sections by visibility in the frame (using the graph
 * search result)
 * - sort the triggered sections by camera distance and possibly also use number
 * of translucent faces as a heuristic for importance
 * - baked models could possibly precompute normals and then just calculate
 * distances when processing a chunk?
 * - many sections can be marked as needing an update but they are only actually
 * scheduled for sorting when the RenderSectionManager makes them visible. This
 * may result in many sections suddenly needing sorting when the camera moves.
 * Maybe it's better to schedule them to be sorted gradually even if not
 * visible, if there are idle threads.
 */
public class GFNI {
    public static final int QUANTIZATION_FACTOR = 4;

    /**
     * A map of all the normal lists, indexed by their normal.
     */
    private Object2ReferenceOpenHashMap<Vector3fc, NormalList> normalLists = new Object2ReferenceOpenHashMap<>(50);

    private Consumer<ChunkSectionPos> triggerSectionCallback;

    // TODO: both of these are for debugging
    ObjectLinkedOpenHashSet<ChunkSectionPos> triggeredSections = new ObjectLinkedOpenHashSet<>(50);
    ObjectOpenHashSet<Vector3fc> triggeredNormals = new ObjectOpenHashSet<>(50);

    public void findTriggeredSections(
            Consumer<ChunkSectionPos> triggerSectionCallback,
            double lastCameraX, double lastCameraY, double lastCameraZ,
            double cameraX, double cameraY, double cameraZ) {
        triggeredSections.clear();
        triggeredNormals.clear();
        this.triggerSectionCallback = triggerSectionCallback;

        for (var normalList : this.normalLists.values()) {
            normalList.processMovement(this, lastCameraX, lastCameraY, lastCameraZ, cameraX, cameraY, cameraZ);
        }

        // TODO: the collections for tracking sections are only used for debugging
        int triggerCount = this.triggeredSections.size();
        if (triggerCount > 0) {
            System.out.println("Triggered " + triggerCount + " sections");
            for (ChunkSectionPos section : this.triggeredSections) {
                System.out.println(section.getX() + " " + section.getY() + " " + section.getZ());
            }
        }

        triggerSectionCallback = null;
    }

    void triggerSection(ChunkSectionPos section, Vector3fc normal) {
        this.triggeredSections.add(section);
        this.triggeredNormals.add(normal);

        // by simply setting a chunk update type on the section, it naturally only gets
        // updated once the section becomes visible.
        // by definition, all sections in GFNI have SortType.DYNAMIC
        this.triggerSectionCallback.accept(section);
    }

    private void removeSectionFromList(NormalList normalList, long chunkSectionLongPos) {
        normalList.removeSection(chunkSectionLongPos);
        if (normalList.isEmpty()) {
            this.normalLists.remove(normalList.getNormal());
        }
    }

    public void removeSection(long chunkSectionLongPos) {
        int normalListCount = this.normalLists.size();

        for (var normalList : this.normalLists.values()) {
            removeSectionFromList(normalList, chunkSectionLongPos);
        }

        // TODO: remove
        if (normalListCount != this.normalLists.size()) {
            System.out.println(normalLists.size() + " normal lists");
        }
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
    public synchronized SortType integrateGroupBuilder(GroupBuilder builder) {
        long chunkSectionLongPos = builder.sectionPos.asLong();

        // if the builder is irrelevant, remove it from the normal lists
        // if the builder is relevant, this may also simplify it as a side effect
        SortType sortType = builder.getSortTypeAndSimplify();
        if (!sortType.needsDynamicSort) {
            removeSection(chunkSectionLongPos);
            return sortType;
        }

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

        return sortType;
    }
}
