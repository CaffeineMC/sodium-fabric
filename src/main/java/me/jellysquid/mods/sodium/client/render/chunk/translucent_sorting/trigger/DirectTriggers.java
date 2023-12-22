package me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import it.unimi.dsi.fastutil.doubles.Double2ObjectRBTreeMap;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TopoSortDynamicData;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger.SortTriggering.SectionTriggers;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Performs direct triggering for sections that are sorted by distance. Direct
 * triggering means the section is not triggered based on its geometry but
 * rather the movement of the camera relative to the last position the camera
 * was in when the section was sorted last.
 * 
 * There are two types of direct triggering: Distance triggering is used when
 * the camera is close to or inside the section. Angle triggering is used
 * otherwise. Distance triggering sorts the section when the camera has moved at
 * least a certain distance from the last sort position while angle triggering
 * sorts it when a certain angle between the current and last sort position is
 * exceeded (relative to the section center).
 */
class DirectTriggers implements SectionTriggers<TopoSortDynamicData> {
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
    private static final double TRIGGER_ANGLE = Math.toRadians(10);
    private static final double EARLY_TRIGGER_ANGLE_COS = Math.cos(TRIGGER_ANGLE * EARLY_TRIGGER_FACTOR);
    private static final double SECTION_CENTER_DIST_SQUARED = 3 * Math.pow(16 / 2, 2) + 1;
    private static final double SECTION_CENTER_DIST = Math.sqrt(SECTION_CENTER_DIST_SQUARED);

    /**
     * How far the player must travel in blocks from the last position at which a
     * section was sorted for it to be sorted again, if direct distance triggering
     * is used (for close sections).
     */
    private static final double DIRECT_TRIGGER_DISTANCE = 1;
    private static final double EARLY_DIRECT_TRIGGER_DISTANCE_SQUARED = Math
            .pow(DIRECT_TRIGGER_DISTANCE * EARLY_TRIGGER_FACTOR, 2);

    int getDirectTriggerCount() {
        return this.directTriggerSections.size();
    }

    private class DirectTriggerData {
        final ChunkSectionPos sectionPos;
        private Vector3dc sectionCenter;
        final TopoSortDynamicData dynamicData;
        DirectTriggerData next;

        /**
         * Absolute camera position at the time of the last trigger.
         */
        Vector3dc triggerCameraPos;

        DirectTriggerData(TopoSortDynamicData dynamicData, ChunkSectionPos sectionPos, Vector3dc triggerCameraPos) {
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

    private static double angleCos(double ax, double ay, double az, double bx, double by, double bz) {
        double lengthA = Math.sqrt(Math.fma(ax, ax, Math.fma(ay, ay, az * az)));
        double lengthB = Math.sqrt(Math.fma(bx, bx, Math.fma(by, by, bz * bz)));
        double dot = Math.fma(ax, bx, Math.fma(ay, by, az * bz));
        return dot / (lengthA * lengthB);
    }

    private void insertDirectAngleTrigger(DirectTriggerData data, Vector3dc cameraPos, double remainingAngle) {
        double triggerCameraSectionCenterDist = data.getSectionCenterTriggerCameraDist();
        double centerMinDistance = Math.tan(remainingAngle) * (triggerCameraSectionCenterDist - SECTION_CENTER_DIST);
        this.insertTrigger(this.accumulatedDistance + centerMinDistance, data);
    }

    private void insertDirectDistanceTrigger(DirectTriggerData data, Vector3dc cameraPos, double remainingDistance) {
        this.insertTrigger(this.accumulatedDistance + remainingDistance, data);
    }

    private void insertTrigger(double key, DirectTriggerData data) {
        data.dynamicData.setDirectTriggerKey(key);

        // attach the previous value after the current one in the list. if there is none
        // it's just null
        data.next = this.directTriggerSections.put(key, data);
    }

    @Override
    public void processTriggers(SortTriggering ts, CameraMovement movement) {
        Vector3dc lastCamera = movement.lastCamera();
        Vector3dc camera = movement.currentCamera();
        this.accumulatedDistance += lastCamera.distance(camera);

        // iterate all elements with a key of at most accumulatedDistance
        var head = this.directTriggerSections.headMap(this.accumulatedDistance);
        for (var entry : head.double2ObjectEntrySet()) {
            this.directTriggerSections.remove(entry.getDoubleKey());
            var data = entry.getValue();
            while (data != null) {
                // get the next element before it's modified by the data being re-inserted into
                // the tree
                var next = data.next;

                if (data.isAngleTriggering(camera)) {
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
                        ts.triggerSectionDirect(data.sectionPos);
                        data.triggerCameraPos = camera;
                    } else {
                        remainingAngle -= Math.acos(angleCos);
                    }

                    this.insertDirectAngleTrigger(data, camera, remainingAngle);
                } else {
                    double remainingDistance = DIRECT_TRIGGER_DISTANCE;
                    double lastTriggerCurrentCameraDistSquared = data.triggerCameraPos.distanceSquared(camera);

                    if (lastTriggerCurrentCameraDistSquared >= EARLY_DIRECT_TRIGGER_DISTANCE_SQUARED) {
                        ts.triggerSectionDirect(data.sectionPos);
                        data.triggerCameraPos = camera;
                    } else {
                        remainingDistance -= Math.sqrt(lastTriggerCurrentCameraDistSquared);
                    }

                    this.insertDirectDistanceTrigger(data, camera, remainingDistance);
                }

                data = next;
            }
        }
    }

    @Override
    public void removeSection(long sectionPos, TranslucentData data) {
        if (data instanceof TopoSortDynamicData triggerable) {
            var key = triggerable.getDirectTriggerKey();
            if (key != -1) {
                this.directTriggerSections.remove(key);
                triggerable.setDirectTriggerKey(-1);
            }
        }
    }

    @Override
    public void addSection(ChunkSectionPos sectionPos, TopoSortDynamicData data, Vector3dc cameraPos) {
        var newData = new DirectTriggerData(data, sectionPos, cameraPos);
        if (newData.isAngleTriggering(cameraPos)) {
            this.insertDirectAngleTrigger(newData, cameraPos, TRIGGER_ANGLE);
        } else {
            this.insertDirectDistanceTrigger(newData, cameraPos, DIRECT_TRIGGER_DISTANCE);
        }
    }
}
