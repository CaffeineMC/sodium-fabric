package me.jellysquid.mods.sodium.client.render.chunk.occlusion;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
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

        this.init(visitor, queue, viewport, origin, searchDistance, useOcclusionCulling, frame);

        while ((section = queue.poll()) != null) {
            var distance = getClosestVertexDistanceToCamera(camera.getPos(), origin, section);

            if (distance > (searchDistance * searchDistance) || section.isOutsideViewport(viewport)) {
                continue;
            }

            visitor.accept(section);

            int connections;

            if (useOcclusionCulling) {
                connections = VisibilityEncoding.getConnections(section.getVisibilityData(), section.getIncomingDirections());
            } else {
                connections = GraphDirectionSet.ALL;
            }

            connections &= getOutwardDirections(origin, section);

            if (connections != GraphDirectionSet.NONE) {
                searchNeighbors(queue, section, connections, frame);
            }
        }
    }

    private static void searchNeighbors(ArrayDeque<RenderSection> queue, RenderSection section, int outgoing, int frame) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            if (!GraphDirectionSet.contains(outgoing, direction)) {
                continue;
            }

            RenderSection adj = section.getAdjacent(direction);

            if (adj != null) {
                enqueue(queue, adj, GraphDirectionSet.of(GraphDirection.opposite(direction)), frame);
            }
        }
    }

    private static void enqueue(ArrayDeque<RenderSection> queue, RenderSection render, int directions, int frame) {
        if (render.getLastVisibleFrame() != frame) {
            render.setLastVisibleFrame(frame);
            render.setIncomingDirections(GraphDirectionSet.NONE);

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

    private void init(Consumer<RenderSection> visitor,
                      ArrayDeque<RenderSection> queue,
                      Viewport viewport,
                      ChunkSectionPos origin,
                      double searchDistance,
                      boolean useOcclusionCulling,
                      int frame)
    {
        if (origin.getY() < this.world.getBottomSectionCoord()) {
            // below the world
            this.initOutsideWorldHeight(queue, viewport, origin, searchDistance, frame,
                    this.world.getBottomSectionCoord(), GraphDirection.DOWN);
        } else if (origin.getY() >= this.world.getTopSectionCoord()) {
            // above the world
            this.initOutsideWorldHeight(queue, viewport, origin, searchDistance, frame,
                    this.world.getTopSectionCoord() - 1, GraphDirection.UP);
        } else {
            this.initWithinWorld(visitor, queue, origin, useOcclusionCulling, frame);
        }
    }

    private void initWithinWorld(Consumer<RenderSection> visitor, ArrayDeque<RenderSection> queue, ChunkSectionPos origin, boolean useOcclusionCulling, int frame) {
        var node = this.getRenderSection(origin.getX(), origin.getY(), origin.getZ());

        if (node == null) {
            return;
        }

        visitor.accept(node);

        int outgoing;

        if (useOcclusionCulling) {
            outgoing = VisibilityEncoding.getConnections(node.getVisibilityData());
        } else {
            outgoing = GraphDirectionSet.ALL;
        }

        if (outgoing != GraphDirectionSet.NONE) {
            searchNeighbors(queue, node, outgoing, frame);
        }
    }

    // Enqueues sections that are inside the viewport using diamond spiral iteration to avoid sorting and ensure a
    // consistent order. Innermost layers are enqueued first. Within each layer, iteration starts at the northernmost
    // section and proceeds counterclockwise (N->W->S->E).
    private void initOutsideWorldHeight(ArrayDeque<RenderSection> queue,
                                        Viewport viewport,
                                        ChunkSectionPos origin,
                                        double searchDistance,
                                        int frame,
                                        int height,
                                        int direction)
    {
        int radius = MathHelper.ceil(searchDistance / 16.0D);

        int originX = origin.getX();
        int originZ = origin.getZ();

        // Layer 0
        this.enqueue(queue, originX, height, originZ, direction, frame, viewport);

        // Complete layers, excluding layer 0
        for (int layer = 1; layer <= radius; layer++) {
            for (int z = -layer; z < layer; z++) {
                int x = Math.abs(z) - layer;
                this.enqueue(queue, originX + x, height, originZ + z, direction, frame, viewport);
            }

            for (int z = layer; z > -layer; z--) {
                int x = layer - Math.abs(z);
                this.enqueue(queue, originX + x, height, originZ + z, direction, frame, viewport);
            }
        }

        // Incomplete layers
        for (int layer = radius + 1; layer <= 2 * radius; layer++) {
            int l = layer - radius;

            for (int z = -radius; z <= -l; z++) {
                int x = -z - layer;
                this.enqueue(queue, originX + x, height, originZ + z, direction, frame, viewport);
            }

            for (int z = l; z <= radius; z++) {
                int x = z - layer;
                this.enqueue(queue, originX + x, height, originZ + z, direction, frame, viewport);
            }

            for (int z = radius; z >= l; z--) {
                int x = layer - z;
                this.enqueue(queue, originX + x, height, originZ + z, direction, frame, viewport);
            }

            for (int z = -l; z >= -radius; z--) {
                int x = layer + z;
                this.enqueue(queue, originX + x, height, originZ + z, direction, frame, viewport);
            }
        }
    }

    private void enqueue(ArrayDeque<RenderSection> queue, int x, int y, int z, int direction, int frame, Viewport viewport) {
        RenderSection section = this.getRenderSection(x, y, z);

        if (section == null || section.isOutsideViewport(viewport)) {
            return;
        }

        enqueue(queue, section, GraphDirectionSet.of(direction), frame);
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sections.get(ChunkSectionPos.asLong(x, y, z));
    }
}
