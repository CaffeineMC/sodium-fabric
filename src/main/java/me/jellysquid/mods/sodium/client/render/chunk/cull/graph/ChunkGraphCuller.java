package me.jellysquid.mods.sodium.client.render.chunk.cull.graph;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.cull.ChunkCuller;
import me.jellysquid.mods.sodium.client.render.chunk.cull.DirectionInt;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import me.jellysquid.mods.sodium.common.util.collections.TrackedArray;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChunkGraphCuller implements ChunkCuller {
    private final Long2ObjectMap<ChunkGraphNode> nodesByPosition = new Long2ObjectOpenHashMap<>();
    private final TrackedArray<ChunkGraphNode> nodes = new TrackedArray<>(ChunkGraphNode.class, 4096);

    private final ChunkGraphIterationQueue iterationQueue = new ChunkGraphIterationQueue();
    private final IntArrayList visible = new IntArrayList();

    private final World world;
    private final int renderDistance;

    private FrustumExtended frustum;
    private boolean useOcclusionCulling;

    private int activeFrame = 0;

    public ChunkGraphCuller(World world, int renderDistance) {
        this.world = world;
        this.renderDistance = renderDistance;
    }

    @Override
    public IntArrayList computeVisible(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.initSearch(camera, frustum, frame, spectator);

        ChunkGraphIterationQueue queue = this.iterationQueue;

        for (int i = 0; i < queue.size(); i++) {
            this.process(queue.getNode(i), queue.getDirection(i), queue.getCullingState(i));
        }

        return this.visible;
    }

    private void initSearch(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.activeFrame = frame;
        this.frustum = frustum;
        this.useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled;

        this.iterationQueue.clear();
        this.visible.clear();

        BlockPos origin = camera.getBlockPos();

        int chunkX = origin.getX() >> 4;
        int chunkY = origin.getY() >> 4;
        int chunkZ = origin.getZ() >> 4;

        ChunkGraphNode rootNode = this.getNode(chunkX, chunkY, chunkZ);

        if (rootNode != null) {
            rootNode.setLastVisibleFrame(frame);

            if (spectator && this.world.getBlockState(origin).isOpaqueFullCube(this.world, origin)) {
                this.useOcclusionCulling = false;
            }

            this.enqueue(rootNode);
        } else {
            chunkY = MathHelper.clamp(origin.getY() >> 4, 0, 15);

            List<ChunkGraphNode> bestNodes = new ArrayList<>();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    ChunkGraphNode node = this.getNode(chunkX + x2, chunkY, chunkZ + z2);

                    if (node == null || node.isCulledByFrustum(frustum)) {
                        continue;
                    }

                    node.setLastVisibleFrame(frame);

                    bestNodes.add(node);
                }
            }

            bestNodes.sort(Comparator.comparingDouble(node -> node.getSquaredDistance(origin)));

            for (ChunkGraphNode node : bestNodes) {
                this.enqueue(node);
            }
        }
    }

    private void process(ChunkGraphNode node, int flow, int cullingState) {
        long visibilityData;

        if (this.useOcclusionCulling && flow != DirectionInt.NULL) {
            visibilityData = node.getVisibilityData() >>> (flow * DirectionInt.COUNT);
        } else {
            visibilityData = Integer.MAX_VALUE;
        }

        long vis = ~cullingState & visibilityData;
        long mask = 1L;

        for (int dir = 0; dir < DirectionInt.COUNT; dir++) {
            if ((vis & mask) != 0L) {
                ChunkGraphNode adj = node.getConnectedNode(dir);

                if (adj != null && adj.getLastVisibleFrame() != this.activeFrame) {
                    adj.setLastVisibleFrame(this.activeFrame);

                    if (!adj.isCulledByFrustum(this.frustum)) {
                        this.enqueue(adj, dir, cullingState);
                    }
                }
            }

            mask <<= 1;
        }
    }

    private void enqueue(ChunkGraphNode node) {
        this.markVisible(node);

        this.iterationQueue.add(node, DirectionInt.NULL);
    }

    private void enqueue(ChunkGraphNode node, int flow, int cullingState) {
        this.markVisible(node);

        this.iterationQueue.add(node, DirectionInt.getOpposite(flow), cullingState);
    }

    private void markVisible(ChunkGraphNode node) {
        if (!node.isEmpty()) {
            this.visible.add(node.getId());
        }
    }

    private void connectNeighborNodes(ChunkGraphNode node) {
        for (int dir : DirectionInt.all()) {
            ChunkGraphNode adj = this.findAdjacentNode(node, DirectionInt.toEnum(dir));

            if (adj != null) {
                adj.setAdjacentNode(DirectionInt.getOpposite(dir), node);
            }

            node.setAdjacentNode(dir, adj);
        }
    }

    private void disconnectNeighborNodes(ChunkGraphNode node) {
        for (int dir : DirectionInt.all()) {
            ChunkGraphNode adj = node.getConnectedNode(dir);

            if (adj != null) {
                adj.setAdjacentNode(DirectionInt.getOpposite(dir), null);
            }

            node.setAdjacentNode(dir, null);
        }
    }

    private ChunkGraphNode findAdjacentNode(ChunkGraphNode node, Direction dir) {
        return this.getNode(node.getChunkX() + dir.getOffsetX(), node.getChunkY() + dir.getOffsetY(), node.getChunkZ() + dir.getOffsetZ());
    }

    private ChunkGraphNode getNode(int x, int y, int z) {
        return this.nodesByPosition.get(ChunkSectionPos.asLong(x, y, z));
    }

    @Override
    public void onSectionStateChanged(int sectionId, ChunkRenderData renderData) {
        ChunkGraphNode node = this.nodes.get(sectionId);
        node.updateRenderData(renderData);
    }

    @Override
    public void onSectionLoaded(int x, int y, int z, int sectionId) {
        ChunkGraphNode node = new ChunkGraphNode(x, y, z, sectionId);
        ChunkGraphNode prev;

        if ((prev = this.nodesByPosition.put(node.getPosition(), node)) != null) {
            this.disconnectNeighborNodes(prev);
        }

        this.nodes.add(node);

        this.connectNeighborNodes(node);
    }

    @Override
    public void onSectionUnloaded(int sectionId) {
        ChunkGraphNode node = this.nodes.remove(sectionId);

        this.nodesByPosition.remove(node.getPosition());

        this.disconnectNeighborNodes(node);
    }

    @Override
    public boolean isSectionVisible(int sectionId) {
        return this.nodes.get(sectionId).getLastVisibleFrame() == this.activeFrame;
    }
}
