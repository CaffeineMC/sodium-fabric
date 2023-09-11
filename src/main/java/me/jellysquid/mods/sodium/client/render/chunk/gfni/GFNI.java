package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.util.function.Consumer;

import org.joml.Vector3fc;

import com.mojang.blaze3d.systems.VertexSorter;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.util.sorting.VertexSorters;
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
     * The vertex sorter for each direction.
     * TODO: is there a better place to put this
     */
    public static final VertexSorter[] SORTERS = new VertexSorter[ModelQuadFacing.DIRECTIONS];

    static {
        for (int i = 0; i < ModelQuadFacing.DIRECTIONS; i++) {
            SORTERS[i] = VertexSorters.sortByAxis(ModelQuadFacing.VALUES[i]);
        }
    }

    // TODO: both of these are for debugging
    ObjectLinkedOpenHashSet<ChunkSectionPos> triggeredSections = new ObjectLinkedOpenHashSet<>(50);
    ObjectOpenHashSet<Vector3fc> triggeredNormals = new ObjectOpenHashSet<>(50);

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

    /**
     * Removes a section from GFNI. This removes all its face planes.
     * 
     * @param chunkSectionLongPos the section to remove
     */
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

    /**
     * Integrates the data from a group builder into GFNI. The group builder
     * contains the translucent face planes of a single section. This method may
     * also remove the section if it has become irrelevant.
     * 
     * TODO: marked as synchronized because it's expected to be fast, see class
     * comment
     * 
     * @param builder the group builder to integrate
     * @return the sort type that the group builder's relevance heuristic determined
     */
    public void integrateTranslucentData(TranslucentData translucentData) {
        if (translucentData == null) {
            return; // TODO: when does this even happen?
        }
        long chunkSectionLongPos = translucentData.sectionPos.asLong();

        // remove the section if the data doesn't need to trigger on face planes
        // TODO: only do the heuristic and topo sort if the hashes are different?
        SortType sortType = translucentData.getSortType();
        if (!sortType.needsPlaneTrigger) {
            removeSection(chunkSectionLongPos);
            return;
        }

        // TODO: implement cycle breaking and multiple sort orders
        if (!(translucentData instanceof DynamicData)) {
            throw new RuntimeException("TranslucentData other than DynamicData passed to GFNI");
        }
        var dynamicData = (DynamicData) translucentData;
        int normalListCount = this.normalLists.size();

        // go through all normal lists and check against the normals that the group
        // builder has. if the normal list has data for the section, but the group
        // builder doesn't, the group is removed. otherwise, the group is updated.
        for (var normalList : this.normalLists.values()) {
            // check if the group builder includes data for this normal.
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

        // go through the data of the group builder to check for data of new normals
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

        // TODO: remove
        if (normalListCount != this.normalLists.size()) {
            System.out.println(normalLists.size() + " normal lists");
        }
    }
}
