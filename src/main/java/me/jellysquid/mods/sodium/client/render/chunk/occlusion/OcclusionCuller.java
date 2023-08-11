package me.jellysquid.mods.sodium.client.render.chunk.occlusion;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
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
                             Viewport viewport,
                             float searchDistance,
                             boolean useOcclusionCulling,
                             int frame) {
        RenderSection section;

        final ArrayDeque<RenderSection> queue = this.queue;

        this.init(visitor, queue, viewport, searchDistance, useOcclusionCulling, frame);

        while ((section = queue.poll()) != null) {
            if (isOutsideRenderDistance(viewport, section, searchDistance)) {
                continue;
            }

            if (isOutsideFrustum(viewport, section)) {
                continue;
            }

            visitor.accept(section);

            int connections;

            {
                if (useOcclusionCulling) {
                    // When using occlusion culling, we can only traverse into neighbors for which there is a path of
                    // visibility through this chunk. This is determined by taking all the incoming paths to this chunk and
                    // creating a union of the outgoing paths from those.
                    connections = VisibilityEncoding.getConnections(section.getVisibilityData(), section.getIncomingDirections());
                } else {
                    // Not using any occlusion culling, so traversing in any direction is legal.
                    connections = GraphDirectionSet.ALL;
                }

                // We can only traverse *outwards* from the center of the graph search, so mask off any invalid
                // directions.
                connections &= getOutwardDirections(viewport.getChunkCoord(), section);
            }

            visitNeighbors(queue, section, connections, frame);
        }
    }

    private static void visitNeighbors(ArrayDeque<RenderSection> queue, RenderSection section, int outgoing, int frame) {
        // Only traverse into neighbors which are actually present.
        // This avoids a null-check on each invocation to enqueue, and since the compiler will see that a null
        // is never encountered (after profiling), it will optimize it away.
        outgoing &= section.getAdjacentMask();

        // Check if there are any valid connections left, and if not, early-exit.
        if (outgoing == GraphDirectionSet.NONE) {
            return;
        }

        if (GraphDirectionSet.contains(outgoing, GraphDirection.DOWN)) {
            visitNeighbor(queue, section.adjacentDown, GraphDirectionSet.of(GraphDirection.UP), frame);
        }

        if (GraphDirectionSet.contains(outgoing, GraphDirection.UP)) {
            visitNeighbor(queue, section.adjacentUp, GraphDirectionSet.of(GraphDirection.DOWN), frame);
        }

        if (GraphDirectionSet.contains(outgoing, GraphDirection.NORTH)) {
            visitNeighbor(queue, section.adjacentNorth, GraphDirectionSet.of(GraphDirection.SOUTH), frame);
        }

        if (GraphDirectionSet.contains(outgoing, GraphDirection.SOUTH)) {
            visitNeighbor(queue, section.adjacentSouth, GraphDirectionSet.of(GraphDirection.NORTH), frame);
        }

        if (GraphDirectionSet.contains(outgoing, GraphDirection.WEST)) {
            visitNeighbor(queue, section.adjacentWest, GraphDirectionSet.of(GraphDirection.EAST), frame);
        }

        if (GraphDirectionSet.contains(outgoing, GraphDirection.EAST)) {
            visitNeighbor(queue, section.adjacentEast, GraphDirectionSet.of(GraphDirection.WEST), frame);
        }
    }

    private static void visitNeighbor(ArrayDeque<RenderSection> queue, @NotNull RenderSection render, int incoming, int frame) {
        if (render.getLastVisibleFrame() != frame) {
            // This is the first time we are visiting this section during the given frame, so we must
            // reset the state.
            render.setLastVisibleFrame(frame);
            render.setIncomingDirections(incoming);

            queue.add(render);
        } else {
            // We have already visited this section during the given frame, so that means this section
            // has multiple incoming paths to it.
            render.addIncomingDirections(incoming);
        }
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
    private static boolean isOutsideRenderDistance(Viewport viewport, RenderSection section, float maxDistance) {
        var origin = viewport.getBlockCoord();
        var transform = viewport.getTransform();

        // The position of the point which will be used for distance calculations
        int pointX = section.getCenterX();
        int pointY = section.getCenterY();
        int pointZ = section.getCenterZ();

        pointX += Integer.signum(origin.getX() - pointX) * 8; // (chunk.x > center.x) ? -8 : +8
        pointY += Integer.signum(origin.getY() - pointY) * 8; // (chunk.y > center.y) ? -8 : +8
        pointZ += Integer.signum(origin.getZ() - pointZ) * 8; // (chunk.z > center.z) ? -8 : +8

        // the vertex's distance from the origin on each axis
        float distanceX = (pointX - transform.intX) - transform.fracX;
        float distanceY = (pointY - transform.intY) - transform.fracY;
        float distanceZ = (pointZ - transform.intZ) - transform.fracZ;

        // vanilla's "cylindrical fog" algorithm
        // max(length(distance.xz), abs(distance.y))
        var distanceSq = Math.max((distanceX * distanceX) + (distanceZ * distanceZ), distanceY * distanceY);
        var distanceLimitSq = MathHelper.square(maxDistance);

        return distanceSq > distanceLimitSq;
    }

    public static boolean isOutsideFrustum(Viewport viewport, RenderSection section) {
        return !viewport.isBoxVisible(section.getCenterX(), section.getCenterY(), section.getCenterZ(), 8.0f);
    }

    private void init(Consumer<RenderSection> visitor,
                      ArrayDeque<RenderSection> queue,
                      Viewport viewport,
                      float searchDistance,
                      boolean useOcclusionCulling,
                      int frame)
    {
        var origin = viewport.getChunkCoord();

        if (origin.getY() < this.world.getBottomSectionCoord()) {
            // below the world
            this.initOutsideWorldHeight(queue, viewport, searchDistance, frame,
                    this.world.getBottomSectionCoord(), GraphDirection.DOWN);
        } else if (origin.getY() >= this.world.getTopSectionCoord()) {
            // above the world
            this.initOutsideWorldHeight(queue, viewport, searchDistance, frame,
                    this.world.getTopSectionCoord() - 1, GraphDirection.UP);
        } else {
            this.initWithinWorld(visitor, queue, viewport, useOcclusionCulling, frame);
        }
    }

    private void initWithinWorld(Consumer<RenderSection> visitor, ArrayDeque<RenderSection> queue, Viewport viewport, boolean useOcclusionCulling, int frame) {
        var origin = viewport.getChunkCoord();
        var node = this.getRenderSection(origin.getX(), origin.getY(), origin.getZ());

        if (node == null) {
            return;
        }

        visitor.accept(node);

        int outgoing;

        if (useOcclusionCulling) {
            // Since the camera is located inside this chunk, there are no "incoming" directions. So we need to instead
            // find any possible paths out of this chunk and enqueue those neighbors.
            outgoing = VisibilityEncoding.getConnections(node.getVisibilityData());
        } else {
            // Occlusion culling is disabled, so we can traverse into any neighbor.
            outgoing = GraphDirectionSet.ALL;
        }

        visitNeighbors(queue, node, outgoing, frame);
    }

    // Enqueues sections that are inside the viewport using diamond spiral iteration to avoid sorting and ensure a
    // consistent order. Innermost layers are enqueued first. Within each layer, iteration starts at the northernmost
    // section and proceeds counterclockwise (N->W->S->E).
    private void initOutsideWorldHeight(ArrayDeque<RenderSection> queue,
                                        Viewport viewport,
                                        float searchDistance,
                                        int frame,
                                        int height,
                                        int direction)
    {
        var origin = viewport.getChunkCoord();
        var radius = MathHelper.ceil(searchDistance / 16.0f);

        // Layer 0
        this.enqueue(queue, origin.getX(), height, origin.getZ(), direction, frame, viewport);

        // Complete layers, excluding layer 0
        for (int layer = 1; layer <= radius; layer++) {
            for (int z = -layer; z < layer; z++) {
                int x = Math.abs(z) - layer;
                this.enqueue(queue, origin.getX() + x, height, origin.getZ() + z, direction, frame, viewport);
            }

            for (int z = layer; z > -layer; z--) {
                int x = layer - Math.abs(z);
                this.enqueue(queue, origin.getX() + x, height, origin.getZ() + z, direction, frame, viewport);
            }
        }

        // Incomplete layers
        for (int layer = radius + 1; layer <= 2 * radius; layer++) {
            int l = layer - radius;

            for (int z = -radius; z <= -l; z++) {
                int x = -z - layer;
                this.enqueue(queue, origin.getX() + x, height, origin.getZ() + z, direction, frame, viewport);
            }

            for (int z = l; z <= radius; z++) {
                int x = z - layer;
                this.enqueue(queue, origin.getX() + x, height, origin.getZ() + z, direction, frame, viewport);
            }

            for (int z = radius; z >= l; z--) {
                int x = layer - z;
                this.enqueue(queue, origin.getX() + x, height, origin.getZ() + z, direction, frame, viewport);
            }

            for (int z = -l; z >= -radius; z--) {
                int x = layer + z;
                this.enqueue(queue, origin.getX() + x, height, origin.getZ() + z, direction, frame, viewport);
            }
        }
    }

    private void enqueue(ArrayDeque<RenderSection> queue, int x, int y, int z, int direction, int frame, Viewport viewport) {
        RenderSection section = this.getRenderSection(x, y, z);

        if (section == null || isOutsideFrustum(viewport, section)) {
            return;
        }

        visitNeighbor(queue, section, GraphDirectionSet.of(direction), frame);
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sections.get(ChunkSectionPos.asLong(x, y, z));
    }
}
