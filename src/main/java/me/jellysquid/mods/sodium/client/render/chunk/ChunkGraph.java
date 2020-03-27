package me.jellysquid.mods.sodium.client.render.chunk;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkRender;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

public class ChunkGraph<T extends ChunkRenderData> implements ChunkStatusListener {
    private final Long2ObjectOpenHashMap<ChunkRender<T>> nodes = new Long2ObjectOpenHashMap<>();

    private final ObjectList<ChunkRender<T>> unloadQueue = new ObjectArrayList<>();
    private final ObjectList<ChunkRender<T>> visibleChunks = new ObjectArrayList<>();
    private final ObjectList<ChunkRender<T>> drawableChunks = new ObjectArrayList<>();

    private final ObjectList<BlockEntity> visibleBlockEntities = new ObjectArrayList<>();

    private final ObjectArrayFIFOQueue<ChunkRender<T>> iterationQueue = new ObjectArrayFIFOQueue<>();

    private final ChunkRenderManager<T> renderManager;
    private final World world;

    private int minX, minZ, maxX, maxZ;
    private int renderDistance;


    public ChunkGraph(ChunkRenderManager<T> renderManager, World world, int renderDistance) {
        this.renderManager = renderManager;
        this.world = world;
        this.renderDistance = renderDistance;
    }

    public void calculateVisible(Camera camera, Vec3d cameraPos, BlockPos blockPos, int frame, Frustum frustum, boolean spectator) {
        int centerX = MathHelper.floor(cameraPos.x) >> 4;
        int centerZ = MathHelper.floor(cameraPos.z) >> 4;

        this.minX = centerX - this.renderDistance;
        this.minZ = centerZ - this.renderDistance;

        this.maxX = centerX + this.renderDistance;
        this.maxZ = centerZ + this.renderDistance;

        this.visibleChunks.clear();
        this.drawableChunks.clear();
        this.visibleBlockEntities.clear();

        boolean cull = this.init(blockPos, camera, cameraPos, frustum, frame, spectator);

        SodiumGameOptions options = SodiumClientMod.options();

        boolean fogCulling = cull && this.renderDistance > 4 && options.quality.enableFog && options.performance.useFogChunkCulling;
        int maxChunkDistance = (this.renderDistance * 16) + 16;

        while (!this.iterationQueue.isEmpty()) {
            ChunkRender<T> render = this.iterationQueue.dequeue();

            this.markVisible(render);

            if (fogCulling && !render.getOrigin().isWithinDistance(cameraPos, maxChunkDistance)) {
                continue;
            }

            this.addNeighbors(render, cull, frustum, frame);
        }
    }

    private void markVisible(ChunkRender<T> render) {
        this.visibleChunks.add(render);

        if (!render.isEmpty()) {
            this.drawableChunks.add(render);
        }

        Collection<BlockEntity> blockEntities = render.getMeshInfo().getBlockEntities();

        if (!blockEntities.isEmpty()) {
            this.visibleBlockEntities.addAll(blockEntities);
        }
    }

    private void addNeighbors(ChunkRender<T> render, boolean cull, Frustum frustum, int frame) {
        for (Direction adjDir : DirectionUtil.ALL_DIRECTIONS) {
            ChunkRender<T> adjRender = this.getAdjacentRender(render, adjDir);

            if (adjRender == null || adjRender.getRebuildFrame() == frame) {
                continue;
            }

            if (cull && !this.isVisible(render, adjRender, adjDir, frustum)) {
                continue;
            }

            if (!adjRender.hasNeighbors()) {
                continue;
            }

            adjRender.setDirection(adjDir);
            adjRender.setRebuildFrame(frame);
            adjRender.updateCullingState(render.cullingState, adjDir);

            this.iterationQueue.enqueue(adjRender);
        }
    }

    private boolean isVisible(ChunkRender<T> render, ChunkRender<T> adjRender, Direction dir, Frustum frustum) {
        if (render.canCull(dir.getOpposite())) {
            return false;
        }

        if (render.direction != null && !render.isVisibleThrough(render.direction.getOpposite(), dir)) {
            return false;
        }

        return frustum.isVisible(adjRender.getBoundingBox());
    }

    private boolean init(BlockPos blockPos, Camera camera, Vec3d cameraPos, Frustum frustum, int frame, boolean spectator) {
        MinecraftClient client = MinecraftClient.getInstance();
        ObjectArrayFIFOQueue<ChunkRender<T>> queue = this.iterationQueue;

        boolean cull = client.chunkCullingEnabled;

        ChunkRender<T> node = this.getOrCreateRender(blockPos);

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
                this.visibleChunks.add(node);
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

            List<ChunkRender<T>> list = Lists.newArrayList();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    ChunkRender<T> chunk = this.getOrCreateRender(new BlockPos(x + (x2 << 4) + 8, y, z + (z2 << 4) + 8));

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

            list.sort(Comparator.comparingDouble(o -> blockPos.getSquaredDistance(o.getOrigin().add(8, 8, 8))));

            for (ChunkRender<T> n : list) {
                queue.enqueue(n);
            }
        }

        return cull;
    }

    public ChunkRender<T> getOrCreateRender(BlockPos pos) {
        return this.getOrCreateRender(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    public ChunkRender<T> getOrCreateRender(int x, int y, int z) {
        if (y < 0 || y >= 16) {
            return null;
        }

        return this.nodes.computeIfAbsent(ChunkSectionPos.asLong(x, y, z), this.renderManager::createChunkRender);
    }

    public ChunkRender<T> getRender(int x, int y, int z) {
        return this.nodes.get(ChunkSectionPos.asLong(x, y, z));
    }

    private ChunkRender<T> getAdjacentRender(ChunkRender<T> render, Direction direction) {
        int x = render.getChunkX() + direction.getOffsetX();
        int y = render.getChunkY() + direction.getOffsetY();
        int z = render.getChunkZ() + direction.getOffsetZ();

        if (!this.isWithinRenderBounds(x, y, z)) {
            return null;
        }

        return render.getAdjacent(direction);
    }

    private boolean isWithinRenderBounds(int x, int y, int z) {
        return y >= 0 && y < 256 && x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
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
        for (ChunkRender<?> node : this.nodes.values()) {
            node.deleteResources();
        }

        this.nodes.clear();
        this.visibleChunks.clear();
    }

    public ObjectList<ChunkRender<T>> getVisibleChunks() {
        return this.visibleChunks;
    }

    public ObjectList<ChunkRender<T>> getDrawableChunks() {
        return this.drawableChunks;
    }

    public boolean cleanup() {
        if (this.unloadQueue.isEmpty()) {
            return false;
        }

        boolean flag = false;

        for (ChunkRender<T> render : this.unloadQueue) {
            render.refreshChunk();

            if (!render.isChunkPresent()) {
                this.removeRenderer(render);

                flag = true;
            }
        }

        this.unloadQueue.clear();

        return flag;
    }

    private void removeRenderer(ChunkRender<T> render) {
        render.deleteResources();

        this.nodes.remove(render.getPositionKey());
    }

    @Override
    public void onChunkAdded(int x, int z) {
        for (int y = 0; y < 16; y++) {
            ChunkRender<T> render = this.getRender(x, y, z);

            if (render != null) {
                render.setChunkPresent(true);
            }
        }
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        for (int y = 0; y < 16; y++) {
            ChunkRender<T> render = this.getRender(x, y, z);

            if (render != null) {
                render.setChunkPresent(false);

                this.unloadQueue.add(render);
            }
        }
    }

    public Collection<BlockEntity> getVisibleBlockEntities() {
        return this.visibleBlockEntities;
    }
}
