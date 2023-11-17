package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting;

import java.util.List;
import java.util.function.BiConsumer;

import org.joml.Vector3dc;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * This class is the central point in translucency sorting. It counts the number
 * of translucent data objects for each sort type and delegates triggering of
 * sections for dynamic sorting to the trigger components.
 * 
 * TODO:
 * - many sections can be marked as needing an update but they are only actually
 * scheduled for sorting when the RenderSectionManager makes them visible. This
 * may result in many sections suddenly needing sorting when the camera moves.
 * Maybe it's better to schedule them to be sorted gradually even if not
 * visible, if there are idle threads.
 * - Groups of quads that form convex shapes in a single plane without holes can
 * be sorted as one "quad". Their internal sorting can be arbitrary. Detecting
 * and grouping/ungrouping them might prove difficult. Finding all quad groups
 * is hard but finding one quad group at a time per direction is doable during
 * building. Using the extents calculated for aligned quads, continuity can be
 * easily tested and then convexity confirmed. How to deal with groups of quads
 * that are only convex if a few of the quads are ignored?
 * - Movement prediction and preemptive task scheduling to avoid needing to
 * perform blocking sorts of close sections. Maybe not an issue? Might reduce
 * stutter in high fps situations. However, high complexity with regards to
 * processing the results of preemptive sorts.
 * - determine the right distance for angle/distance triggering. It seems just
 * the diagonal of a section is too small, angle triggering is broken at close
 * distances.
 * 
 * @author douira
 */
public class TranslucentSorting {
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
     * To avoid generating a collection of the triggered sections, this callback is
     * used to process the triggered sections directly as they are queried from the
     * normal lists' interval trees. The callback is given the section coordinates,
     * and a boolean indicating if the trigger was an direct trigger.
     */
    private BiConsumer<Long, Boolean> triggerSectionCallback;

    /**
     * The number of triggered sections and normals. The normals are kept in a
     * hashmap to count them, triggered sections are not deduplicated.
     */
    private int gfniTriggerCount = 0;
    private int directTriggerCount = 0;
    private final IntOpenHashSet triggeredNormals = new IntOpenHashSet();
    private int triggeredNormalCount = 0;

    /**
     * A map of the number of times each sort type is currently in use.
     */
    private final int[] sortTypeCounters = new int[SortType.values().length];

    private final GFNITriggers gfni = new GFNITriggers();
    private final DirectTriggers direct = new DirectTriggers();

    interface SectionTriggers {
        void processTriggers(TranslucentSorting ts, CameraMovement movement);

        void removeSection(long sectionPos, TranslucentData data);

        void addSection(ChunkSectionPos sectionPos, DynamicData data, Vector3dc cameraPos);
    }

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
    public void triggerSections(BiConsumer<Long, Boolean> triggerSectionCallback, CameraMovement movement) {
        triggeredNormals.clear();
        this.triggerSectionCallback = triggerSectionCallback;
        var oldGfniTriggerCount = this.gfniTriggerCount;
        var oldDirectTriggerCount = this.directTriggerCount;
        this.gfniTriggerCount = 0;
        this.directTriggerCount = 0;

        this.gfni.processTriggers(this, movement);
        this.direct.processTriggers(this, movement);

        if (this.gfniTriggerCount > 0 || this.directTriggerCount > 0) {
            this.triggeredNormalCount = this.triggeredNormals.size();
        } else {
            this.gfniTriggerCount = oldGfniTriggerCount;
            this.directTriggerCount = oldDirectTriggerCount;
        }

        triggerSectionCallback = null;
    }

    void triggerSectionGFNI(long sectionPos, int collectorKey) {
        this.triggeredNormals.add(collectorKey);
        this.triggerSectionCallback.accept(sectionPos, false);
        this.gfniTriggerCount++;
    }

    void triggerSectionDirect(ChunkSectionPos sectionPos) {
        this.triggerSectionCallback.accept(sectionPos.asLong(), true);
        this.directTriggerCount++;
    }

    public void applyTriggerChanges(DynamicData data, ChunkSectionPos pos, Vector3dc cameraPos) {
        if (data.turnGFNITriggerOff) {
            this.gfni.removeSection(pos.asLong(), data);
        }
        if (data.turnDirectTriggerOn) {
            this.direct.addSection(pos, data, cameraPos);
        }
        if (data.turnDirectTriggerOff) {
            this.direct.removeSection(pos.asLong(), data);
        }
        data.clearTriggerChanges();
    }

    private void decrementSortTypeCounter(TranslucentData oldData) {
        if (oldData != null) {
            this.sortTypeCounters[oldData.getSortType().ordinal()]--;
        }
    }

    private void incrementSortTypeCounter(TranslucentData newData) {
        this.sortTypeCounters[newData.getSortType().ordinal()]++;
    }

    /**
     * Removes a section from direct and GFNI triggering. This removes all its face
     * planes.
     * 
     * @param oldData    the data of the section to remove
     * @param sectionPos the section to remove
     */
    public void removeSection(TranslucentData oldData, long sectionPos) {
        if (oldData == null) {
            return;
        }
        this.gfni.removeSection(sectionPos, oldData);
        this.direct.removeSection(sectionPos, oldData);
        this.decrementSortTypeCounter(oldData);
    }

    /**
     * Integrates the data from a geometry collector into GFNI. The geometry
     * collector contains the translucent face planes of a single section. This
     * method may also remove the section if it has become irrelevant.
     * 
     * @param builder the geometry collector to integrate
     * @return the sort type that the geometry collector's relevance heuristic
     *         determined
     */
    public void integrateTranslucentData(TranslucentData oldData, TranslucentData newData, Vector3dc cameraPos) {
        if (oldData == newData) {
            return;
        }

        var pos = newData.sectionPos;

        this.incrementSortTypeCounter(newData);

        // remove the section if the data doesn't need to trigger on face planes
        if (newData instanceof DynamicData dynamicData) {
            this.direct.removeSection(pos.asLong(), oldData);
            this.decrementSortTypeCounter(oldData);
            if (dynamicData.GFNITrigger) {
                this.gfni.addSection(pos, dynamicData, cameraPos);
            } else {
                // remove the collector since this section is never going to get gfni triggering
                // (there's no option to add sections to GFNI later currently)
                dynamicData.deleteCollector();
            }
            if (dynamicData.directTrigger) {
                this.direct.addSection(pos, dynamicData, cameraPos);
            }

            // clear trigger changes on data change because the current state of trigger
            // types was just set
            dynamicData.clearTriggerChanges();
        } else {
            this.removeSection(oldData, pos.asLong());
            return;
        }
    }

    public void addDebugStrings(List<String> list) {
        list.add(String.format("TS NL=%02d TrN=%02d TrS=G%03d/D%03d",
                this.gfni.getUniqueNormalCount(),
                this.triggeredNormalCount,
                this.gfniTriggerCount,
                this.directTriggerCount));
        list.add(String.format("N=%05d SNR=%05d STA=%04d DYN=%04d (DIR=%04d)",
                this.sortTypeCounters[SortType.NONE.ordinal()],
                this.sortTypeCounters[SortType.STATIC_NORMAL_RELATIVE.ordinal()],
                this.sortTypeCounters[SortType.STATIC_TOPO_ACYCLIC.ordinal()],
                this.sortTypeCounters[SortType.DYNAMIC_ALL.ordinal()],
                this.direct.getDirectTriggerCount()));
    }
}
