package me.jellysquid.mods.sodium.client.render.chunk.gfni;

import java.util.List;
import java.util.function.BiConsumer;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
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
 * - Does fluid renderer sometimes produce aligned faces that it doesn't
 * classify as such? they could be detected and then correctly assigned in the
 * fluid renderer directly.
 * - Check that separators still work on aligned geomtry. It seems that a lot of
 * things are not topo sortable after the most recent fix to the quad visibility
 * check.
 * - figure out why when the player moves at high speed there end up being more
 * direct trigger entries than sections with the dynamic sort type, even if
 * everything is set to only direct trigger and not GFNI trigger. (the numbers
 * should be the same, somehow they're not being bookkept correctly)
 * - determine the right distance for angle/distance triggering. It seems just
 * the diagonal of a section is too small, angle triggering is broken at close
 * distances.
 * - separate this class into GFNI and direct triggering?
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
     * A tree map of the directly triggered sections, indexed by their
     * minimum required camera movement. When the given camera movement is exceeded,
     * they are tested for triggering the angle or distance condition.
     * 
     * The accumulated distance is monotonically increasing and is never reset. This
     * only becomes a problem when the camera moves more than 10^15 blocks in total.
     * There will be precision issues at around 10^10 maybe, but it's still not a
     * concern.
     */
    private Double2ObjectRBTreeMap<DirectTriggerData> directTriggerSections = new Double2ObjectRBTreeMap<>();
    private double accumulatedDistance = 0;

    /**
     * To avoid generating a collection of the triggered sections, this callback is
     * used to process the triggered sections directly as they are queried from the
     * normal lists' interval trees. The callback is given the section coordinates,
     * and a boolean indicating if the trigger was an direct trigger.
     */
    private BiConsumer<Long, Boolean> triggerSectionCallback;

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

    private void decrementSortTypeCounter(TranslucentData oldData) {
        if (oldData != null) {
            this.sortTypeCounters[oldData.getSortType().ordinal()]--;
        }
    }

    private void incrementSortTypeCounter(TranslucentData newData) {
        this.sortTypeCounters[newData.getSortType().ordinal()]++;
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
        triggeredSections.clear();
        triggeredNormals.clear();
        this.triggerSectionCallback = triggerSectionCallback;

        processGFNITriggers(movement);
        processDirectTriggers(movement);

        var newTriggeredSectionsCount = this.triggeredSections.size();
        var newTriggeredNormalCount = this.triggeredNormals.size();
        if (newTriggeredSectionsCount > 0) {
            this.triggeredSectionCount = newTriggeredSectionsCount;
            this.triggeredNormalCount = newTriggeredNormalCount;
        }

        // int triggerCount = this.triggeredSections.size();
        // if (triggerCount > 0) {
        // System.out.println("Triggered " + triggerCount + " sections");
        // for (long section : this.triggeredSections) {
        // ChunkSectionPos sectionPos = ChunkSectionPos.from(section);
        // System.out.println(sectionPos.getX() + " " + sectionPos.getY() + " " +
        // sectionPos.getZ());
        // }
        // }

        triggerSectionCallback = null;
    }

    private void processGFNITriggers(CameraMovement movement) {
        for (var normalList : this.normalLists.values()) {
            normalList.processMovement(this, movement);
        }
    }

    /**
     * The factor by which the trigger distance and angle are multiplied to get a
     * smaller threshold that is used for the actual trigger check but not the
     * remaining distance calculation. This prevents curved movements from taking
     * sections out of the tree and re-inserting without triggering many times near
     * the actual trigger distance. It cuts the repeating unsuccessful trigger
     * attempts off early.
     */
    private static final double EARLY_TRIGGER_FACTOR = 0.9;

    /**
     * Degrees of movement from last sort position before the section is sorted
     * again.
     */
    private static final double TRIGGER_ANGLE = Math.toRadians(20);
    private static final double EARLY_TRIGGER_ANGLE_COS = Math.cos(TRIGGER_ANGLE * EARLY_TRIGGER_FACTOR);
    private static final double SECTION_CENTER_DIST_SQUARED = 40 * 3 * Math.pow(16 / 2, 2);
    private static final double SECTION_CENTER_DIST = Math.sqrt(SECTION_CENTER_DIST_SQUARED);

    /**
     * How far the player must travel in blocks from the last position at which a
     * section was sorted for it to be sorted again, if direct distance triggering
     * is used (for close sections).
     */
    private static final double DIRECT_TRIGGER_DISTANCE = 1;
    private static final double EARLY_DIRECT_TRIGGER_DISTANCE_SQUARED = Math
            .pow(DIRECT_TRIGGER_DISTANCE * EARLY_TRIGGER_FACTOR, 2);

    private class DirectTriggerData {
        final ChunkSectionPos sectionPos;
        private Vector3dc sectionCenter;
        final DynamicData dynamicData;

        /**
         * Absolute camera position at the time of the last trigger.
         */
        Vector3dc triggerCameraPos;

        DirectTriggerData(DynamicData dynamicData, ChunkSectionPos sectionPos, Vector3dc triggerCameraPos) {
            this.dynamicData = dynamicData;
            this.sectionPos = sectionPos;
            this.triggerCameraPos = triggerCameraPos;
        }

        Vector3dc getSectionCenter() {
            if (this.sectionCenter == null) {
                this.sectionCenter = new Vector3d(
                        sectionPos.getMinX() + 8,
                        sectionPos.getMinY() + 8,
                        sectionPos.getMinZ() + 8);
            }
            return this.sectionCenter;
        }

        /**
         * Returns the distance between the sort camera pos and the center of the
         * section.
         */
        double getSectionCenterTriggerCameraDist() {
            return Math.sqrt(getSectionCenterDistSquared(this.triggerCameraPos));
        }

        double getSectionCenterDistSquared(Vector3dc vector) {
            Vector3dc sectionCenter = getSectionCenter();
            return sectionCenter.distanceSquared(vector);
        }

        boolean isAngleTriggering(Vector3dc vector) {
            return getSectionCenterDistSquared(vector) > SECTION_CENTER_DIST_SQUARED;
        }
    }

    // TODO: use faster code for this
    private static double angleCos(double ax, double ay, double az, double bx, double by, double bz) {
        double length1Squared = Math.fma(ax, ax, Math.fma(ay, ay, az * az));
        double length2Squared = Math.fma(bx, bx, Math.fma(by, by, bz * bz));
        double dot = Math.fma(ax, bx, Math.fma(ay, by, az * bz));
        return dot / Math.sqrt(length1Squared * length2Squared);
    }

    private void processDirectTriggers(CameraMovement movement) {
        Vector3dc lastCamera = movement.lastCamera();
        Vector3dc camera = movement.currentCamera();
        this.accumulatedDistance += lastCamera.distance(camera);

        // iterate all elements with a key of at most accumulatedDistance
        var head = this.directTriggerSections.headMap(this.accumulatedDistance);
        for (var entry : head.double2ObjectEntrySet()) {
            this.directTriggerSections.remove(entry.getDoubleKey());
            DirectTriggerData data = entry.getValue();

            boolean isAngle = data.isAngleTriggering(camera);
            if (isAngle) {
                double remainingAngle = TRIGGER_ANGLE;

                // check if the angle since the last sort exceeds the threshold
                Vector3dc sectionCenter = data.getSectionCenter();
                double angleCos = angleCos(
                        sectionCenter.x() - data.triggerCameraPos.x(),
                        sectionCenter.y() - data.triggerCameraPos.y(),
                        sectionCenter.z() - data.triggerCameraPos.z(),
                        sectionCenter.x() - camera.x(),
                        sectionCenter.y() - camera.y(),
                        sectionCenter.z() - camera.z());

                // compare angles inverted because cosine flips it
                if (angleCos <= EARLY_TRIGGER_ANGLE_COS) {
                    this.triggerSectionDirect(data.sectionPos);
                    data.triggerCameraPos = camera;
                } else {
                    remainingAngle -= Math.acos(angleCos);
                }

                insertDirectAngleTrigger(data, camera, remainingAngle);
            } else {
                double remainingDistance = DIRECT_TRIGGER_DISTANCE;
                double lastTriggerCurrentCameraDistSquared = data.triggerCameraPos.distanceSquared(camera);

                if (lastTriggerCurrentCameraDistSquared >= EARLY_DIRECT_TRIGGER_DISTANCE_SQUARED) {
                    this.triggerSectionDirect(data.sectionPos);
                    data.triggerCameraPos = camera;
                } else {
                    remainingDistance -= Math.sqrt(lastTriggerCurrentCameraDistSquared);
                }

                insertDirectDistanceTrigger(data, camera, remainingDistance);
            }
        }
    }

    private void insertDirectAngleTrigger(DirectTriggerData data, Vector3dc cameraPos, double remainingAngle) {
        double triggerCameraSectionCenterDist = data.getSectionCenterTriggerCameraDist();
        double centerMinDistance = Math.tan(remainingAngle) * (triggerCameraSectionCenterDist - SECTION_CENTER_DIST);
        insertJitteredTrigger(this.accumulatedDistance + centerMinDistance, data);
    }

    private void insertDirectDistanceTrigger(DirectTriggerData data, Vector3dc cameraPos, double remainingDistance) {
        insertJitteredTrigger(this.accumulatedDistance + remainingDistance, data);
    }

    // jitters the double values to never overwrite the same key. abuses that
    // doubles have more precision than we need to make them a kind of hash table
    private double lastJittered = -1;
    private double lastJitterResult = -1;

    private void insertJitteredTrigger(double key, DirectTriggerData data) {
        // attempt insert without jittering
        if (this.directTriggerSections.putIfAbsent(key, data) == null) {
            data.dynamicData.directTriggerKey = key;
            return;
        }

        // if this is the same key as last time, skip all the identical keys that have
        // already been jittered
        if (this.lastJittered == key) {
            key = this.lastJitterResult;
        } else {
            this.lastJittered = key;
        }

        // Go to the next lower double to avoid collisions in the map. This slightly
        // changes the key but doesn't significantly change its value. Subtraction is
        // necessary to avoid delaying a trigger for too long. It's ok if it's too early
        // though. This approach brings with it the disadvantages of linear probing but
        // bad cases are unlikely to happen so it's fine.
        do {
            key = Math.nextDown(key);
        } while (this.directTriggerSections.putIfAbsent(key, data) != null);
        data.dynamicData.directTriggerKey = key;
        this.lastJitterResult = key;
    }

    void triggerSectionGFNI(long sectionPos, int collectorKey) {
        this.triggeredNormals.add(collectorKey);
        this.triggeredSections.add(sectionPos);
        this.triggerSectionCallback.accept(sectionPos, false);
    }

    private void triggerSectionDirect(ChunkSectionPos sectionPos) {
        var sectionPosLong = sectionPos.asLong();
        this.triggerSectionCallback.accept(sectionPosLong, true);
        this.triggeredSections.add(sectionPosLong);
    }

    public void applyTriggerChanges(DynamicData data, ChunkSectionPos pos, Vector3dc cameraPos) {
        if (data.turnGFNITriggerOff) {
            disableGFNITriggering(data, pos.asLong());
        }
        if (data.turnDirectTriggerOn) {
            enableDirectTriggering(data, pos, cameraPos);
        }
        if (data.turnDirectTriggerOff) {
            disableDirectTriggering(data);
        }
        data.clearTriggerChanges();
    }

    private void enableDirectTriggering(DynamicData data, ChunkSectionPos section, Vector3dc cameraPos) {
        var newData = new DirectTriggerData(data, section, cameraPos);
        if (newData.isAngleTriggering(cameraPos)) {
            insertDirectAngleTrigger(newData, cameraPos, TRIGGER_ANGLE);
        } else {
            insertDirectDistanceTrigger(newData, cameraPos, DIRECT_TRIGGER_DISTANCE);
        }
    }

    private void disableDirectTriggering(TranslucentData data) {
        if (data instanceof DynamicData dynamicData) {
            var key = dynamicData.directTriggerKey;
            if (key != -1) {
                this.directTriggerSections.remove(key);
                dynamicData.directTriggerKey = -1;
            }
        }
    }

    /**
     * Removes a section from direct and GFNI triggering. This removes all its face
     * planes.
     * 
     * @param oldData    the data of the section to remove
     * @param sectionPos the section to remove
     */
    public void removeSection(TranslucentData oldData, long sectionPos) {
        disableGFNITriggering(oldData, sectionPos);
        disableDirectTriggering(oldData);
        decrementSortTypeCounter(oldData);
    }

    private void addSectionInNewNormalLists(DynamicData dynamicData, AccumulationGroup accGroup) {
        var normal = accGroup.normal;
        var normalList = this.normalLists.get(normal);
        if (normalList == null) {
            normalList = new NormalList(normal, accGroup.collectorKey);
            this.normalLists.put(normal, normalList);
            normalList.addSection(accGroup, accGroup.sectionPos.asLong());
        }
    }

    private void removeSectionFromList(NormalList normalList, long sectionPos) {
        normalList.removeSection(sectionPos);
        if (normalList.isEmpty()) {
            this.normalLists.remove(normalList.getNormal());
        }
    }

    private void disableGFNITriggering(TranslucentData oldData, long sectionPos) {
        for (var normalList : this.normalLists.values()) {
            removeSectionFromList(normalList, sectionPos);
        }
    }

    private void initiallyEnableGFNITriggering(DynamicData data, long sectionPos) {
        var collector = data.getCollector();

        // go through all normal lists and check against the normals that the group
        // builder has. if the normal list has data for the section, but the group
        // builder doesn't, the group is removed. otherwise, the group is updated.
        for (var normalList : this.normalLists.values()) {
            // check if the geometry collector includes data for this normal.
            var accGroup = collector.getGroupForNormal(normalList);
            if (normalList.hasSection(sectionPos)) {
                if (accGroup == null) {
                    removeSectionFromList(normalList, sectionPos);
                } else {
                    normalList.updateSection(accGroup, sectionPos);
                }
            } else if (accGroup != null) {
                normalList.addSection(accGroup, sectionPos);
            }
        }

        // go through the data of the geometry collector to check for data of new
        // normals
        // for which there are no normal lists yet. This only checks for new normal
        // lists since new data for existing normal lists is handled above.
        if (collector.axisAlignedDistances != null) {
            for (var accGroup : collector.axisAlignedDistances) {
                if (accGroup != null) {
                    addSectionInNewNormalLists(data, accGroup);
                }
            }
        }
        if (collector.unalignedDistances != null) {
            for (var accGroup : collector.unalignedDistances.values()) {
                addSectionInNewNormalLists(data, accGroup);
            }
        }

        data.deleteCollector();
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
        long sectionPos = newData.sectionPos.asLong();

        incrementSortTypeCounter(newData);

        // remove the section if the data doesn't need to trigger on face planes
        // TODO: only do the heuristic and topo sort if the hashes are different?
        if (newData instanceof DynamicData dynamicData) {
            disableDirectTriggering(oldData);
            decrementSortTypeCounter(oldData);
            if (dynamicData.GFNITrigger) {
                initiallyEnableGFNITriggering(dynamicData, sectionPos);
            } else {
                // remove the collector since this section is never going to get gfni triggering
                // (there's no option to add sections to GFNI later currently)
                dynamicData.deleteCollector();
            }
            if (dynamicData.directTrigger) {
                enableDirectTriggering(dynamicData, newData.sectionPos, cameraPos);
            }
        } else {
            removeSection(oldData, sectionPos);
            return;
        }
    }

    public void addDebugStrings(List<String> list) {
        list.add("GFNI NL=" + this.normalLists.size()
                + " TrS=" + this.triggeredSectionCount
                + " TrN=" + this.triggeredNormalCount);
        list.add("N=" + this.sortTypeCounters[SortType.NONE.ordinal()]
                + " SNR=" + this.sortTypeCounters[SortType.STATIC_NORMAL_RELATIVE.ordinal()]
                + " STA=" + this.sortTypeCounters[SortType.STATIC_TOPO_ACYCLIC.ordinal()]
                + " DYN=" + this.sortTypeCounters[SortType.DYNAMIC_ALL.ordinal()]
                + " (DIR=" + this.directTriggerSections.size() + ")");
    }
}
