package me.jellysquid.mods.sodium.client.render.chunk;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

public class ChunkGraph {
    private static final Direction[] DIRECTIONS = Direction.values();

    private final ObjectList<ChunkGraphNode> visibleNodes = new ObjectArrayList<>();

    private final ChunkGraphNodeStorage nodes;
    private final World world;
    private final ObjectArrayFIFOQueue<ChunkGraphNode> iterationQueue = new ObjectArrayFIFOQueue<>();

    private int minX, minZ, maxX, maxZ;
    private int renderDistance;

    private ChunkStatusTracker statusTracker;

    public ChunkGraph(World world, ExtendedBuiltChunkStorage chunks, ChunkStatusTracker statusTracker, int renderDistance) {
        this.world = world;
        this.renderDistance = renderDistance;
        this.statusTracker = statusTracker;
        this.nodes = new ChunkGraphNodeStorage(renderDistance, chunks.getData());
    }

    public void calculateVisible(Camera camera, Vec3d cameraPos, BlockPos blockPos, int frame, Frustum frustum, boolean spectator) {
        BlockPos center = new BlockPos(MathHelper.floor(cameraPos.x / 16.0D) * 16,
                MathHelper.floor(cameraPos.y / 16.0D) * 16,
                MathHelper.floor(cameraPos.z / 16.0D) * 16);

        this.minX = center.getX() - (this.renderDistance * 16);
        this.minZ = center.getZ() - (this.renderDistance * 16);

        this.maxX = center.getX() + (this.renderDistance * 16);
        this.maxZ = center.getZ() + (this.renderDistance * 16);

        this.reset();

        boolean cull = this.init(blockPos, camera, cameraPos, frustum, frame, spectator);

        SodiumGameOptions options = SodiumClientMod.options();

        boolean fogCulling = cull && this.renderDistance > 4 && options.quality.enableFog && options.performance.useFogChunkCulling;
        int maxChunkDistance = (this.renderDistance * 16) + 16;

        ObjectArrayFIFOQueue<ChunkGraphNode> queue = this.iterationQueue;

        while (!queue.isEmpty()) {
            ChunkGraphNode node = this.iterationQueue.dequeue();

            this.visibleNodes.add(node);

            if (fogCulling && !node.getOrigin().isWithinDistance(cameraPos, maxChunkDistance)) {
                continue;
            }

            Direction dir = node.direction;

            for (Direction adjDir : DIRECTIONS) {
                ChunkGraphNode adjNode = this.getAdjacentChunk(node, adjDir);

                if (adjNode == null || adjNode.getRebuildFrame() == frame) {
                    continue;
                }

                if (cull) {
                    if (node.canCull(adjDir.getOpposite())) {
                        continue;
                    }

                    if (dir != null && !node.isVisibleThrough(dir.getOpposite(), adjDir)) {
                        continue;
                    }

                    if (!frustum.isVisible(adjNode.getBoundingBox())) {
                        continue;
                    }
                }

                if (!this.statusTracker.areChunksAvailable(adjNode.getIndex())) {
                    continue;
                }

                adjNode.setDirection(adjDir);
                adjNode.setRebuildFrame(frame);
                adjNode.updateCullingState(node.cullingState, adjDir);

                queue.enqueue(adjNode);
            }
        }
    }

    private boolean init(BlockPos blockPos, Camera camera, Vec3d cameraPos, Frustum frustum, int frame, boolean spectator) {
        MinecraftClient client = MinecraftClient.getInstance();
        ObjectArrayFIFOQueue<ChunkGraphNode> queue = this.iterationQueue;

        boolean cull = client.chunkCullingEnabled;

        ChunkGraphNode node = this.nodes.get(blockPos);

        if (node != null) {
            node.reset();

            // Player is within bounds and inside a node
            Set<Direction> openFaces = this.getOpenChunkFaces(blockPos);

            if (openFaces.size() == 1) {
                Vector3f vector3f = camera.getHorizontalPlane();
                Direction direction = Direction.getFacing(vector3f.getX(), vector3f.getY(), vector3f.getZ()).getOpposite();

                openFaces.remove(direction);
            }

            if (openFaces.isEmpty() && !spectator) {
                this.visibleNodes.add(node);
            } else {
                if (spectator && this.world.getBlockState(blockPos).isFullOpaque(this.world, blockPos)) {
                    cull = false;
                }

                node.setRebuildFrame(frame);
                queue.enqueue(node);
            }
        } else {
            // Player is out-of-bounds
            int y = blockPos.getY() > 0 ? 248 : 8;

            int x = MathHelper.floor(cameraPos.x / 16.0D) * 16;
            int z = MathHelper.floor(cameraPos.z / 16.0D) * 16;

            List<ChunkGraphNode> list = Lists.newArrayList();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    ChunkGraphNode chunk = this.nodes.get(new BlockPos(x + (x2 << 4) + 8, y, z + (z2 << 4) + 8));

                    if (chunk == null) {
                        continue;
                    }

                    if (frustum.isVisible(chunk.getBoundingBox())) {
                        chunk.setRebuildFrame(frame);
                        chunk.reset();

                        list.add(chunk);
                    }
                }
            }

            list.sort(Comparator.comparingDouble(o -> blockPos.getSquaredDistance(o.chunk.getOrigin().add(8, 8, 8))));

            for (ChunkGraphNode n : list) {
                queue.enqueue(n);
            }
        }

        return cull;
    }

    private ChunkGraphNode getAdjacentChunk(ChunkGraphNode node, Direction direction) {
        BlockPos chunkPos = node.chunk.getNeighborPosition(direction);

        if (chunkPos.getX() < this.minX || chunkPos.getX() > this.maxX) {
            return null;
        }

        if (chunkPos.getZ() < this.minZ || chunkPos.getZ() > this.maxZ) {
            return null;
        }

        if (chunkPos.getY() < 0 || chunkPos.getY() >= 256) {
            return null;
        }

        return this.nodes.get(chunkPos);
    }

    private Set<Direction> getOpenChunkFaces(BlockPos pos) {
        ChunkOcclusionDataBuilder occlusionBuilder = new ChunkOcclusionDataBuilder();

        WorldChunk chunk = this.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);

        ChunkSection section = chunk.getSectionArray()[pos.getY() >> 4];

        if (section != null && !section.isEmpty()) {
            BlockPos.Mutable mpos = new BlockPos.Mutable();

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, y, z);
                        mpos.set(x, y, z);

                        if (state.isFullOpaque(this.world, mpos)) {
                            occlusionBuilder.markClosed(mpos);
                        }
                    }
                }
            }
        }

        return occlusionBuilder.getOpenFaces(pos);
    }

    public void reset() {
        this.visibleNodes.clear();
    }

    public ObjectList<ChunkGraphNode> getVisibleChunks() {
        return this.visibleNodes;
    }

    public int getVisibleChunkCount() {
        return this.visibleNodes.size();
    }
}
