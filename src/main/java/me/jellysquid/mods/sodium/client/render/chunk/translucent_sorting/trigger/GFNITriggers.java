package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import org.joml.Vector3dc;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.DynamicData;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger.SortTriggering.SectionTriggers;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Performs Global Face Normal Indexing-based triggering as described in
 * https://hackmd.io/@douira100/sodium-sl-gfni
 * 
 * Note on precision: Global distances are stored as doubles while
 * section-relative distances are stored as floats. The distances of the camera
 * are calculated as doubles, but using float normal vectors (furthermore normal
 * vectors are heavily quantized, so angular precision is not a concern).
 */
class GFNITriggers implements SectionTriggers<DynamicData> {
    /**
     * A map of all the normal lists, indexed by their normal.
     */
    private Object2ReferenceOpenHashMap<Vector3fc, NormalList> normalLists = new Object2ReferenceOpenHashMap<>();

    int getUniqueNormalCount() {
        return this.normalLists.size();
    }

    @Override
    public void processTriggers(SortTriggering ts, CameraMovement movement) {
        for (var normalList : this.normalLists.values()) {
            normalList.processMovement(ts, movement);
        }
    }

    private void addSectionInNewNormalLists(DynamicData dynamicData, NormalPlanes normalPlanes) {
        var normal = normalPlanes.normal;
        var normalList = this.normalLists.get(normal);
        if (normalList == null) {
            normalList = new NormalList(normal);
            this.normalLists.put(normal, normalList);
            normalList.addSection(normalPlanes, normalPlanes.sectionPos.asLong());
        }
    }

    /**
     * Removes the section from the normal list and returns whether the normal list
     * is now empty and should itself be removed from the normal lists map. This is
     * done with a return value so that the iterator can be used to remove it safely
     * without a concurrent modification.
     */
    private boolean removeSectionFromList(NormalList normalList, long sectionPos) {
        normalList.removeSection(sectionPos);
        return normalList.isEmpty();
    }

    @Override
    public void removeSection(long sectionPos, TranslucentData data) {
        var iterator = this.normalLists.values().iterator();
        while (iterator.hasNext()) {
            var normalList = iterator.next();
            if (this.removeSectionFromList(normalList, sectionPos)) {
                iterator.remove();
            }
        }
    }

    @Override
    public void addSection(ChunkSectionPos pos, DynamicData data, Vector3dc cameraPos) {
        long sectionPos = pos.asLong();
        var geometryPlanes = data.getGeometryPlanes();

        // go through all normal lists and check against the normals that the group
        // builder has. if the normal list has data for the section, but the group
        // builder doesn't, the group is removed. otherwise, the group is updated.
        var iterator = this.normalLists.values().iterator();
        while (iterator.hasNext()) {
            var normalList = iterator.next();

            // check if the geometry collector includes data for this normal.
            var normalPlanes = geometryPlanes.getPlanesForNormal(normalList);
            if (normalList.hasSection(sectionPos)) {
                if (normalPlanes == null) {
                    if (this.removeSectionFromList(normalList, sectionPos)) {
                        iterator.remove();
                    }
                } else {
                    normalList.updateSection(normalPlanes, sectionPos);
                }
            } else if (normalPlanes != null) {
                normalList.addSection(normalPlanes, sectionPos);
            }
        }

        // go through the data of the geometry collector to check for data of new
        // normals
        // for which there are no normal lists yet. This only checks for new normal
        // lists since new data for existing normal lists is handled above.
        var aligned = geometryPlanes.getAligned();
        if (aligned != null) {
            for (var normalPlane : aligned) {
                if (normalPlane != null) {
                    this.addSectionInNewNormalLists(data, normalPlane);
                }
            }
        }
        var unaligned = geometryPlanes.getUnaligned();
        if (unaligned != null) {
            for (var normalPlane : unaligned) {
                this.addSectionInNewNormalLists(data, normalPlane);
            }
        }

        data.clearGeometryPlanes();
    }
}
