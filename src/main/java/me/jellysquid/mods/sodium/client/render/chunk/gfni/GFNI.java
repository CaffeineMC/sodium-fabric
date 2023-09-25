package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.util.List;
import java.util.function.Consumer;

import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Global Face Normal Indexing as described in
 * https://hackmd.io/@douira100/sodium-sl-gfni
 * 
 * Distances are stored as doubles and normals are stored as float vectors.
 * 
 * TODO:
 * - sort the triggered sections by camera distance and possibly also use number
 * of translucent faces as a heuristic for importance
 * - baked models could possibly precompute normals and then just calculate
 * distances when processing a chunk?
 * - many sections can be marked as needing an update but they are only actually
 * scheduled for sorting when the RenderSectionManager makes them visible. This
 * may result in many sections suddenly needing sorting when the camera moves.
 * Maybe it's better to schedule them to be sorted gradually even if not
 * visible, if there are idle threads.
 * - problem: when there are two parallel planes made up of many quads each,
 * sorting the whole thing from one perspective means the sorting is wrong when
 * we look at it from another perspective. This happens because there is no
 * trigger in the plane. Sorting as if the entire thing was one quad would fix it.
 * 
 * @author douira
 */
public class GFNI {
    /**
     * The quantization factor with which the normals are quantized such that there
     * are fewer possible unique normals. The factor describes the number of steps
     * in each direction per dimension that the components of the normals can have.
     * It determines the density of the grid on the surface of a unit cube centered
     * at the origin onto which the normals are projected. The normals are snapped
     * to the nearest grid point.
     */
    public static final int QUANTIZATION_FACTOR = 4;

    /**
     * A map of all the normal lists, indexed by their normal.
     */
    private Object2ReferenceOpenHashMap<Vector3fc, NormalList> normalLists = new Object2ReferenceOpenHashMap<>(50);

    /**
     * To avoid generating a collection of the triggered sections, this callback is
     * used to process the triggered sections directly as they are queried from the
     * normal lists' interval trees.
     */
    private Consumer<ChunkSectionPos> triggerSectionCallback;

    /**
     * A set of all the sections that were triggered the last time something was
     * triggered.
     * 
     * TODO: only count the sections and not their whole position
     */
    private final LongOpenHashSet triggeredSections = new LongOpenHashSet(50);
    private int triggeredSectionCount = 0;

    /**
     * A set of all the normals that were triggered the last time something was
     * triggered.
     */
    private final IntOpenHashSet triggeredNormals = new IntOpenHashSet(50);
    private int triggeredNormalCount = 0;

    /**
     * A map of the number of times each sort type is currently in use.
     */
    private final int[] sortTypeCounters = new int[SortType.values().length];

    /**
     * Triggers the sections that the given camera movement crosses face planes of.
     * 
     * @param triggerSectionCallback called for each section that is triggered
     * @param lastCameraX            the camera x position before the movement
     * @param lastCameraY            the camera y position before the movement
     * @param lastCameraZ            the camera z position before the movement
     * @param cameraX                the camera x position after the movement
     * @param cameraY                the camera y position after the movement
     * @param cameraZ                the camera z position after the movement
     */
    public void triggerSections(
            Consumer<ChunkSectionPos> triggerSectionCallback,
            double lastCameraX, double lastCameraY, double lastCameraZ,
            double cameraX, double cameraY, double cameraZ) {
        triggeredSections.clear();
        triggeredNormals.clear();
        this.triggerSectionCallback = triggerSectionCallback;

        for (var normalList : this.normalLists.values()) {
            normalList.processMovement(this, lastCameraX, lastCameraY, lastCameraZ, cameraX, cameraY, cameraZ);
        }

        var newTriggeredSectionsCount = this.triggeredSections.size();
        var newTriggeredNormalCount = this.triggeredNormals.size();
        if (newTriggeredSectionsCount > 0) {
            this.triggeredSectionCount = newTriggeredSectionsCount;
            this.triggeredNormalCount = newTriggeredNormalCount;
        }

        // int triggerCount = this.triggeredSections.size();
        // if (triggerCount > 0) {
        //     System.out.println("Triggered " + triggerCount + " sections");
        //     for (long section : this.triggeredSections) {
        //         ChunkSectionPos sectionPos = ChunkSectionPos.from(section);
        //         System.out.println(sectionPos.getX() + " " + sectionPos.getY() + " " + sectionPos.getZ());
        //     }
        // }

        triggerSectionCallback = null;
    }

    void triggerSection(ChunkSectionPos section, int collectorKey) {
        this.triggeredSections.add(section.asLong());
        this.triggeredNormals.add(collectorKey);

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

    private void decrementSortTypeCounter(TranslucentData oldTranslucentData) {
        if (oldTranslucentData != null) {
            this.sortTypeCounters[oldTranslucentData.getSortType().ordinal()]--;
        }
    }

    /**
     * Removes a section from GFNI. This removes all its face planes.
     * 
     * @param oldTranslucentData  the data of the section to remove
     * @param chunkSectionLongPos the section to remove
     */
    public void removeSection(TranslucentData oldTranslucentData, long chunkSectionLongPos) {
        for (var normalList : this.normalLists.values()) {
            removeSectionFromList(normalList, chunkSectionLongPos);
        }

        decrementSortTypeCounter(oldTranslucentData);
    }

    private void addSectionInNewNormalLists(AccumulationGroup accGroup) {
        var normal = accGroup.normal;
        var normalList = this.normalLists.get(normal);
        if (normalList == null) {
            normalList = new NormalList(normal, accGroup.collectorKey);
            this.normalLists.put(normal, normalList);
            normalList.addSection(accGroup, accGroup.sectionPos.asLong());
        }
    }

    /**
     * Integrates the data from a geometry collector into GFNI. The geometry collector
     * contains the translucent face planes of a single section. This method may
     * also remove the section if it has become irrelevant.
     * 
     * @param builder the geometry collector to integrate
     * @return the sort type that the geometry collector's relevance heuristic determined
     */
    public void integrateTranslucentData(TranslucentData oldTranslucentData, TranslucentData translucentData) {
        long chunkSectionLongPos = translucentData.sectionPos.asLong();

        // remove the section if the data doesn't need to trigger on face planes
        // TODO: only do the heuristic and topo sort if the hashes are different?
        SortType sortType = translucentData.getSortType();
        this.sortTypeCounters[sortType.ordinal()]++;
        if (!sortType.needsPlaneTrigger) {
            removeSection(oldTranslucentData, chunkSectionLongPos);
            return;
        }

        decrementSortTypeCounter(oldTranslucentData);

        // TODO: implement cycle breaking and multiple sort orders
        if (!(translucentData instanceof DynamicData)) {
            throw new RuntimeException("Dynamic topo sort not implemented yet");
        }
        var dynamicData = (DynamicData) translucentData;

        // go through all normal lists and check against the normals that the group
        // builder has. if the normal list has data for the section, but the group
        // builder doesn't, the group is removed. otherwise, the group is updated.
        for (var normalList : this.normalLists.values()) {
            // check if the geometry collector includes data for this normal.
            var accGroup = dynamicData.getGroupForNormal(normalList);
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

        // go through the data of the geometry collector to check for data of new normals
        // for which there are no normal lists yet. This only checks for new normal
        // lists since new data for existing normal lists is handled above.
        if (dynamicData.getAxisAlignedDistances() != null) {
            for (var accGroup : dynamicData.getAxisAlignedDistances()) {
                if (accGroup != null) {
                    addSectionInNewNormalLists(accGroup);
                }
            }
        }
        if (dynamicData.getUnalignedDistances() != null) {
            for (var accGroup : dynamicData.getUnalignedDistances().values()) {
                addSectionInNewNormalLists(accGroup);
            }
        }
    }

    public void addDebugStrings(List<String> list) {
        list.add("GFNI NL=" + this.normalLists.size()
                + " TrS=" + this.triggeredSectionCount
                + " TrN=" + this.triggeredNormalCount);
        list.add("N=" + this.sortTypeCounters[SortType.NONE.ordinal()]
                + " SNR=" + this.sortTypeCounters[SortType.STATIC_NORMAL_RELATIVE.ordinal()]
                + " STA=" + this.sortTypeCounters[SortType.STATIC_TOPO_ACYCLIC.ordinal()]
                + " DYN=" + this.sortTypeCounters[SortType.DYNAMIC_ALL.ordinal()]);
    }
}
