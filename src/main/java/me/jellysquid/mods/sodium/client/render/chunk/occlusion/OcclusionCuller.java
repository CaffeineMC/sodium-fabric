package me.jellysquid.mods.sodium.client.render.chunk.occlusion;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.util.sorting.MergeSort;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class OcclusionCuller {
    private final Long2ReferenceMap<RenderSection> sections;
    private final World world;

    private final ArrayDeque<RenderSection> queue = new ArrayDeque<>();

    public OcclusionCuller(Long2ReferenceMap<RenderSection> sections, World world) {
        this.sections = sections;
        this.world = world;
    }

    public void searchChunks(Consumer<RenderSection> visitor,
                             Camera camera,
                             Viewport viewport,
                             double searchDistance,
                             boolean useOcclusionCulling,
                             int frame) {
        RenderSection section;

        var origin = ChunkSectionPos.from(camera.getBlockPos());

        final ArrayDeque<RenderSection> queue = this.queue;

        this.init(queue, viewport, camera, origin, searchDistance, frame);

        while ((section = queue.poll()) != null) {
            if (origin.getX() != section.getChunkX() || origin.getY() != section.getChunkY() || origin.getZ() != section.getChunkZ()) {
                var distance = getClosestVertexDistanceToCamera(camera.getPos(), origin, section);

                if (distance > (searchDistance * searchDistance) || section.isOutsideViewport(viewport)) {
                    continue;
                }
            }

            visitor.accept(section);

            int connections;

            if (useOcclusionCulling) {
                connections = VisibilityEncoding.getConnections(section.getVisibilityData(), section.getIncomingDirections());
            } else {
                connections = GraphDirection.ALL;
            }

            connections &= getOutwardDirections(origin, section);

            if (connections != GraphDirection.NONE) {
                searchNeighbors(queue, section, connections, frame);
            }
        }
    }

    private static void searchNeighbors(ArrayDeque<RenderSection> queue, RenderSection section, int outgoing, int frame) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            if ((outgoing & (1 << direction)) == 0) {
                continue;
            }

            RenderSection adj = section.getAdjacent(direction);

            if (adj != null) {
                enqueue(queue, adj, 1 << GraphDirection.opposite(direction), frame);
            }
        }
    }

    private static void enqueue(ArrayDeque<RenderSection> queue, RenderSection render, int directions, int frame) {
        if (render.getLastVisibleFrame() != frame) {
            render.setLastVisibleFrame(frame);
            render.setIncomingDirections(GraphDirection.NONE);

            queue.add(render);
        }

        render.addIncomingDirections(directions);
    }

    private static int getOutwardDirections(ChunkSectionPos origin, RenderSection section) {
        int planes = 0;

        planes |= section.getChunkX() <= origin.getX() ? 1 << GraphDirection.WEST  : 0;
        planes |= section.getChunkX() >= origin.getX() ? 1 << GraphDirection.EAST  : 0;

        planes |= section.getChunkY() <= origin.getY() ? 1 << GraphDirection.DOWN  : 0;
        planes |= section.getChunkY() >= origin.getY() ? 1 << GraphDirection.UP    : 0;

        planes |= section.getChunkZ() <= origin.getZ() ? 1 << GraphDirection.NORTH : 0;
        planes |= section.getChunkZ() >= origin.getZ() ? 1 << GraphDirection.SOUTH : 0;

        return planes;
    }

    // picks the closest vertex to the camera of the chunk render bounds, and returns the distance of the vertex from
    // the camera position
    private static double getClosestVertexDistanceToCamera(Vec3d camera, ChunkSectionPos origin, RenderSection section) {
        // the offset of the vertex from the center of the chunk
        int offsetX = Integer.signum(origin.getX() - section.getChunkX()) * 8; // (chunk.x > center.x) ? -8 : +8
        int offsetY = Integer.signum(origin.getY() - section.getChunkY()) * 8; // (chunk.y > center.y) ? -8 : +8
        int offsetZ = Integer.signum(origin.getZ() - section.getChunkZ()) * 8; // (chunk.z > center.z) ? -8 : +8

        // the vertex's distance from the origin on each axis
        double distanceX = camera.x - (section.getCenterX() + offsetX);
        double distanceY = camera.y - (section.getCenterY() + offsetY);
        double distanceZ = camera.z - (section.getCenterZ() + offsetZ);

        // vanilla's "cylindrical fog" algorithm
        // max(length(distance.xz), abs(distance.y))
        return Math.max((distanceX * distanceX) + (distanceZ * distanceZ), distanceY * distanceY);
    }

    private void init(ArrayDeque<RenderSection> queue,
                      Viewport viewport,
                      Camera camera,
                      ChunkSectionPos origin,
                      double searchDistance,
                      int frame)
    {
        if (origin.getY() < this.world.getBottomSectionCoord()) {
            // above the world
            this.initOutsideWorldHeight(queue, viewport, camera, origin, searchDistance, frame,
                    this.world.getBottomSectionCoord(), GraphDirection.DOWN);
        } else if (origin.getY() >= this.world.getTopSectionCoord()) {
            // below the world
            this.initOutsideWorldHeight(queue, viewport, camera, origin, searchDistance, frame,
                    this.world.getTopSectionCoord() - 1, GraphDirection.UP);
        } else {
            var node = this.getRenderSection(origin.getX(), origin.getY(), origin.getZ());

            if (node != null) {
                // within a loaded section
                enqueue(queue, node, GraphDirection.ALL, frame);
            }
        }
    }

    private void initOutsideWorldHeight(ArrayDeque<RenderSection> queue,
                                        Viewport viewport,
                                        Camera camera,
                                        ChunkSectionPos origin,
                                        double searchDistance,
                                        int frame,
                                        int height,
                                        int direction)
    {
        List<RenderSection> sections = new ArrayList<>();
        int radius = MathHelper.ceil(searchDistance / 16.0D);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                RenderSection section = this.getRenderSection(origin.getX() + x, height, origin.getZ() + z);

                if (section == null || section.isOutsideViewport(viewport)) {
                    continue;
                }

                sections.add(section);
            }
        }

        if (!sections.isEmpty()) {
            enqueueAll(queue, sections, camera, direction, frame);
        }
    }

    private static void enqueueAll(ArrayDeque<RenderSection> queue,
                                   List<RenderSection> sections,
                                   Camera camera,
                                   int direction,
                                   int frame)
    {
        final var distance = new float[sections.size()];
        final var origin = camera.getBlockPos();

        for (int index = 0; index < sections.size(); index++) {
            var section = sections.get(index);
            distance[index] = -section.getSquaredDistance(origin); // sort by closest to camera
        }

        // TODO: avoid indirect sort via indices
        for (int index : MergeSort.mergeSort(distance)) {
            enqueue(queue, sections.get(index), 1 << direction, frame);
        }
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sections.get(ChunkSectionPos.asLong(x, y, z));
    }
}
