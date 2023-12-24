package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import java.util.List;
import java.util.function.BiConsumer;

import org.joml.Vector3dc;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions.SortBehavior;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.AlignableNormal;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicData;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TopoSortDynamicData;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * This class is a central point in translucency sorting. It counts the number
 * of translucent data objects for each sort type and delegates triggering of
 * sections for dynamic sorting to the trigger components.
 * 
 * TODO: see if the duplicated geometry bug happens when:
 * - hashing for triggering is removed
 * - hashing for translucent data reuse is removed
 * 
 * @author douira (the translucent_sorting package)
 */
public class SortTriggering {
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
    private final ObjectOpenHashSet<AlignableNormal> triggeredNormals = new ObjectOpenHashSet<>();
    private int triggeredNormalCount = 0;

    /**
     * A map of the number of times each sort type is currently in use.
     */
    private final int[] sortTypeCounters = new int[SortType.values().length];

    private final GFNITriggers gfni = new GFNITriggers();
    private final DirectTriggers direct = new DirectTriggers();

    interface SectionTriggers<T extends DynamicData> {
        void processTriggers(SortTriggering ts, CameraMovement movement);

        void removeSection(long sectionPos, TranslucentData data);

        void addSection(ChunkSectionPos sectionPos, T data, Vector3dc cameraPos);
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

    void triggerSectionGFNI(long sectionPos, AlignableNormal normal) {
        this.triggeredNormals.add(normal);
        this.triggerSectionCallback.accept(sectionPos, false);
        this.gfniTriggerCount++;
    }

    void triggerSectionDirect(ChunkSectionPos sectionPos) {
        this.triggerSectionCallback.accept(sectionPos.asLong(), true);
        this.directTriggerCount++;
    }

    public void applyTriggerChanges(TopoSortDynamicData data, ChunkSectionPos pos, Vector3dc cameraPos) {
        if (data.getAndFlushTurnGFNITriggerOff()) {
            this.gfni.removeSection(pos.asLong(), data);
        }
        if (data.getAndFlushTurnDirectTriggerOn()) {
            this.direct.addSection(pos, data, cameraPos);
        }
        if (data.getAndFlushTurnDirectTriggerOff()) {
            this.direct.removeSection(pos.asLong(), data);
        }
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

        if (newData instanceof DynamicData dynamicData) {
            this.direct.removeSection(pos.asLong(), oldData);
            this.decrementSortTypeCounter(oldData);

            if (dynamicData instanceof TopoSortDynamicData topoSortData) {
                if (topoSortData.GFNITriggerEnabled()) {
                    this.gfni.addSection(pos, topoSortData, cameraPos);
                } else {
                    // remove the trigger data since this section is never going to get gfni
                    // triggering (there's no option to add sections to GFNI later currently)
                    topoSortData.clearGeometryPlanes();
                }
                if (topoSortData.directTriggerEnabled()) {
                    this.direct.addSection(pos, topoSortData, cameraPos);
                }

                // clear trigger changes on data change because the current state of trigger
                // types was just applied
                topoSortData.clearTriggerChanges();
            } else {
                this.gfni.addSection(pos, dynamicData, cameraPos);
            }
        } else {
            this.removeSection(oldData, pos.asLong());
            return;
        }
    }

    public void addDebugStrings(List<String> list) {
        var sortBehavior = SodiumClientMod.options().performance.sortBehavior;
        if (sortBehavior == SortBehavior.OFF) {
            list.add("TS OFF");
        } else {
            list.add("TS (%s) NL=%02d TrN=%02d TrS=G%03d/D%03d".formatted(
                    sortBehavior.getShortName(),
                    this.gfni.getUniqueNormalCount(),
                    this.triggeredNormalCount,
                    this.gfniTriggerCount,
                    this.directTriggerCount));
            list.add("N=%05d SNR=%05d STA=%04d DYN=%04d (DIR=%04d)".formatted(
                    this.sortTypeCounters[SortType.NONE.ordinal()],
                    this.sortTypeCounters[SortType.STATIC_NORMAL_RELATIVE.ordinal()],
                    this.sortTypeCounters[SortType.STATIC_TOPO.ordinal()],
                    this.sortTypeCounters[SortType.DYNAMIC.ordinal()],
                    this.direct.getDirectTriggerCount()));
        }
    }
}
