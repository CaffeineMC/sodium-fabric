package me.jellysquid.mods.sodium.client.render.chunk;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ChunkGraph<T extends ChunkRenderData> {
    private final Long2ObjectOpenHashMap<ChunkGraphNode<T>> nodes = new Long2ObjectOpenHashMap<>();
    private final ObjectList<ChunkGraphNode<T>> visibleNodes = new ObjectArrayList<>();

    private final ChunkRenderManager<T> renderManager;
    private final World world;
    private final ObjectArrayFIFOQueue<ChunkGraphNode<T>> iterationQueue = new ObjectArrayFIFOQueue<>();

    private int minX, minZ, maxX, maxZ;
    private int renderDistance;

    public ChunkGraph(ChunkRenderManager<T> renderManager, World world, int renderDistance) {
        this.renderManager = renderManager;
        this.world = world;
        this.renderDistance = renderDistance;
    }

    public void calculateVisible(Camera camera, Vec3d cameraPos, BlockPos blockPos, int frame, Frustum frustum, boolean spectator) {
        BlockPos center = new BlockPos(MathHelper.floor(cameraPos.x / 16.0D) * 16,
                MathHelper.floor(cameraPos.y / 16.0D) * 16,
                MathHelper.floor(cameraPos.z / 16.0D) * 16);

        this.minX = center.getX() - (this.renderDistance * 16);
        this.minZ = center.getZ() - (this.renderDistance * 16);

        this.maxX = center.getX() + (this.renderDistance * 16);
        this.maxZ = center.getZ() + (this.renderDistance * 16);

        this.visibleNodes.clear();

        boolean cull = this.init(blockPos, camera, cameraPos, frustum, frame, spectator);

        SodiumGameOptions options = SodiumClientMod.options();

        boolean fogCulling = cull && this.renderDistance > 4 && options.quality.enableFog && options.performance.useFogChunkCulling;
        int maxChunkDistance = (this.renderDistance * 16) + 16;

        ObjectArrayFIFOQueue<ChunkGraphNode<T>> queue = this.iterationQueue;

        while (!queue.isEmpty()) {
            ChunkGraphNode<T> node = this.iterationQueue.dequeue();

            this.visibleNodes.add(node);

            if (fogCulling && !node.getOrigin().isWithinDistance(cameraPos, maxChunkDistance)) {
                continue;
            }

            Direction dir = node.direction;

            for (Direction adjDir : DirectionUtil.ALL_DIRECTIONS) {
                ChunkGraphNode<T> adjNode = this.getAdjacentChunk(node, adjDir);

                if (adjNode == null) {
                    continue;
                }

                if (adjNode.getRebuildFrame() == frame) {
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

                if (!adjNode.hasNeighbors()) {
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
        ObjectArrayFIFOQueue<ChunkGraphNode<T>> queue = this.iterationQueue;

        boolean cull = client.chunkCullingEnabled;

        ChunkGraphNode<T> node = this.getOrCreateNode(blockPos);

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

            List<ChunkGraphNode<T>> list = Lists.newArrayList();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    ChunkGraphNode<T> chunk = this.getOrCreateNode(new BlockPos(x + (x2 << 4) + 8, y, z + (z2 << 4) + 8));

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

            for (ChunkGraphNode<T> n : list) {
                queue.enqueue(n);
            }
        }

        return cull;
    }

    private ChunkGraphNode<T> getOrCreateNode(BlockPos pos) {
        return this.nodes.computeIfAbsent(ChunkSectionPos.asLong(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4), this::createNode);
    }

    private ChunkGraphNode<T> createNode(long pos) {
        return new ChunkGraphNode<>(this.renderManager.createChunkRender(ChunkSectionPos.getX(pos) << 4, ChunkSectionPos.getY(pos) << 4, ChunkSectionPos.getZ(pos) << 4));
    }

    private ChunkGraphNode<T> getAdjacentChunk(ChunkGraphNode<T> node, Direction direction) {
        int x = node.chunk.getChunkX() + direction.getOffsetX();
        int y = node.chunk.getChunkY() + direction.getOffsetY();
        int z = node.chunk.getChunkZ() + direction.getOffsetZ();

        if (x < this.minX || x > this.maxX) {
            return null;
        }

        if (z < this.minZ || z > this.maxZ) {
            return null;
        }

        if (y < 0 || y >= 256) {
            return null;
        }

        return this.nodes.computeIfAbsent(ChunkSectionPos.asLong(x, y, z), this::createNode);
    }

    private Set<Direction> getOpenChunkFaces(BlockPos pos) {
        WorldChunk chunk = this.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);

        ChunkSection section = chunk.getSectionArray()[pos.getY() >> 4];

        if (section == null || section.isEmpty()) {
            return EnumSet.allOf(Direction.class);
        }

        ChunkOcclusionDataBuilder occlusionBuilder = new ChunkOcclusionDataBuilder();

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

        return occlusionBuilder.getOpenFaces(pos);
    }

    public void reset() {
        for (ChunkGraphNode<?> node : this.nodes.values()) {
            node.delete();
        }

        this.nodes.clear();
        this.visibleNodes.clear();
    }

    public ObjectList<ChunkGraphNode<T>> getVisibleChunks() {
        return this.visibleNodes;
    }

    public int getVisibleChunkCount() {
        return this.visibleNodes.size();
    }

    public ChunkGraphNode<T> getChunkRender(int x, int y, int z) {
        return this.nodes.get(ChunkSectionPos.asLong(x, y, z));
    }
}
