package net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import java.util.List;
import java.util.function.BiConsumer;

import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortBehavior;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicData;
import org.joml.Vector3dc;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.AlignableNormal;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.SortType;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicTopoData;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import net.minecraft.core.SectionPos;

/**
 * This class is a central point in translucency sorting. It counts the number
 * of translucent data objects for each sort type and delegates triggering of
 * sections for dynamic sorting to the trigger components.
 * 
 * TODO:
 * - investigate why there's a similar number of STA and DYN sections. This might be normal, the counters might be broken or the heuristic is actually wrong.
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
     * The dynamic data being caught up. When a section is rebuilt (initially or
     * later) it might not have the required trigger data registered yet so that it
     * might miss being triggered between being scheduled for rebuild and being
     * integrated. This is solved by catching up the section being integrated with
     * the movement that has happened in the mean time.
     */
    private DynamicData catchupData = null;

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

        void integrateSection(SortTriggering ts, SectionPos sectionPos, T data, CameraMovement movement);
    }

    /**
     * Triggers the sections that the given camera movement crosses face planes of.
     * 
     * @param triggerSectionCallback called for each section that is triggered
     * @param movement               the camera movement to trigger for
     */
    public void triggerSections(BiConsumer<Long, Boolean> triggerSectionCallback, CameraMovement movement) {
        this.triggeredNormals.clear();
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

        this.triggerSectionCallback = null;
    }

    private boolean isCatchingUp() {
        return this.catchupData != null;
    }

    void triggerSectionGFNI(long sectionPos, AlignableNormal normal) {
        if (this.isCatchingUp()) {
            this.triggerSectionCatchup(sectionPos, false);
            return;
        }

        this.triggeredNormals.add(normal);
        this.triggerSectionCallback.accept(sectionPos, false);
        this.gfniTriggerCount++;
    }

    void triggerSectionDirect(SectionPos sectionPos) {
        if (this.isCatchingUp()) {
            this.triggerSectionCatchup(sectionPos.asLong(), true);
            return;
        }

        this.triggerSectionCallback.accept(sectionPos.asLong(), true);
        this.directTriggerCount++;
    }

    private void triggerSectionCatchup(long sectionPos, boolean isDirectTrigger) {
        // catchup triggering might be disabled
        if (this.triggerSectionCallback != null) {
            // do prepareTrigger here since it can't be done through the render section as
            // it hasn't been put there yet or it contains an old data object
            this.catchupData.prepareTrigger(isDirectTrigger);

            // schedule the section to be re-sorted
            this.triggerSectionCallback.accept(sectionPos, isDirectTrigger);
        }
    }

    public void applyTriggerChanges(DynamicTopoData data, DynamicTopoData.DynamicTopoSorter topoSorter, SectionPos pos, Vector3dc cameraPos) {
        if (!data.isMatchingSorter(topoSorter)) {
            return;
        }

        if (data.checkAndApplyGFNITriggerOff(topoSorter)) {
            this.gfni.removeSection(pos.asLong(), data);
        }
        if (data.checkAndApplyDirectTriggerOn(topoSorter)) {
            // use dummy camera movement since there's no risk of the camera moving between
            // the section being scheduled and integrated (there's no building going on
            // here)
            this.direct.integrateSection(this, pos, data, new CameraMovement(cameraPos, cameraPos));
        }
        if (data.checkAndApplyDirectTriggerOff(topoSorter)) {
            this.direct.removeSection(pos.asLong(), data);
        }

        data.applyTopoSortFailureCounterChange(topoSorter);
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
     */
    public void integrateTranslucentData(TranslucentData oldData, TranslucentData newData, Vector3dc cameraPos,
                                         BiConsumer<Long, Boolean> triggerSectionCallback) {
        if (oldData == newData) {
            return;
        }

        var pos = newData.sectionPos;

        this.incrementSortTypeCounter(newData);

        if (newData instanceof DynamicData dynamicData) {
            this.direct.removeSection(pos.asLong(), oldData);
            this.decrementSortTypeCounter(oldData);
            this.triggerSectionCallback = triggerSectionCallback;
            this.catchupData = dynamicData;
            var movement = new CameraMovement(dynamicData.getInitialCameraPos(), cameraPos);

            if (dynamicData instanceof DynamicTopoData topoSortData) {
                if (topoSortData.GFNITriggerEnabled()) {
                    this.gfni.integrateSection(this, pos, topoSortData, movement);
                } else {
                    // remove the trigger data since this section is never going to get gfni
                    // triggering (there's no option to add sections to GFNI later currently)
                    topoSortData.discardGeometryPlanes();
                }
                if (topoSortData.directTriggerEnabled()) {
                    this.direct.integrateSection(this, pos, topoSortData, movement);
                }
            } else {
                this.gfni.integrateSection(this, pos, dynamicData, movement);
            }

            this.triggerSectionCallback = null;
            this.catchupData = null;
        } else {
            this.removeSection(oldData, pos.asLong());
        }
    }

    public void addDebugStrings(List<String> list) {
        var sortBehavior = SodiumClientMod.options().performance.getSortBehavior();
        if (sortBehavior.getSortMode() == SortBehavior.SortMode.NONE) {
            list.add("TS OFF");
        } else {
            list.add("TS (%s) NL=%02d TrN=%02d TrS=G%03d/D%03d".formatted(
                    sortBehavior.getShortName(),
                    this.gfni.getUniqueNormalCount(),
                    this.triggeredNormalCount,
                    this.gfniTriggerCount,
                    this.directTriggerCount));
            list.add("N=%05d SNR=%05d STA=%05d DYN=%05d (DIR=%02d)".formatted(
                    this.sortTypeCounters[SortType.NONE.ordinal()],
                    this.sortTypeCounters[SortType.STATIC_NORMAL_RELATIVE.ordinal()],
                    this.sortTypeCounters[SortType.STATIC_TOPO.ordinal()],
                    this.sortTypeCounters[SortType.DYNAMIC.ordinal()],
                    this.direct.getDirectTriggerCount()));
        }
    }
}
