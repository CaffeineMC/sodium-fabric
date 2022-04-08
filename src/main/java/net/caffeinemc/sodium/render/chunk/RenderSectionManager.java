package net.caffeinemc.sodium.render.chunk;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.*;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.SodiumWorldRenderer;
import net.caffeinemc.sodium.render.chunk.compile.ChunkBuilder;
import net.caffeinemc.sodium.render.chunk.compile.tasks.AbstractBuilderTask;
import net.caffeinemc.sodium.render.chunk.compile.tasks.EmptyTerrainBuildTask;
import net.caffeinemc.sodium.render.chunk.compile.tasks.TerrainBuildResult;
import net.caffeinemc.sodium.render.chunk.compile.tasks.TerrainBuildTask;
import net.caffeinemc.sodium.render.chunk.draw.*;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.passes.DefaultRenderPasses;
import net.caffeinemc.sodium.render.chunk.region.RenderRegionManager;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderData;
import net.caffeinemc.sodium.render.sequence.SequenceBuilder;
import net.caffeinemc.sodium.render.sequence.SequenceIndexBuffer;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexFormats;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.render.texture.SpriteUtil;
import net.caffeinemc.sodium.util.ListUtil;
import net.caffeinemc.sodium.util.MathUtil;
import net.caffeinemc.sodium.util.collections.BitArray;
import net.caffeinemc.sodium.util.tasks.WorkStealingFutureDrain;
import net.caffeinemc.sodium.world.ChunkStatus;
import net.caffeinemc.sodium.world.ChunkTracker;
import net.caffeinemc.sodium.world.slice.WorldSliceData;
import net.caffeinemc.sodium.world.slice.cloned.ClonedChunkSectionCache;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RenderSectionManager {
    /**
     * The maximum distance a chunk can be from the player's camera in order to be eligible for blocking updates.
     */
    private static final float NEARBY_BLOCK_UPDATE_DISTANCE = 32.0f;

    private final ChunkBuilder builder;

    private final RenderRegionManager regions;
    private final ClonedChunkSectionCache sectionCache;

    private final ChunkTree tree;
    private final int chunkViewDistance;

    private final Map<ChunkUpdateType, PriorityQueue<RenderSection>> rebuildQueues = new EnumMap<>(ChunkUpdateType.class);

    private final Map<ChunkRenderPass, ChunkRenderer> chunkRenderers;

    private final ClientWorld world;

    private final SequenceIndexBuffer indexBuffer;

    private boolean needsUpdate;
    private int currentFrame = 0;

    private final ChunkTracker tracker;
    private final RenderDevice device;

    private Map<ChunkRenderPass, ChunkPrep.PreparedRenderList> renderLists;
    private ChunkCameraContext camera;

    private final ReferenceArrayList<RenderSection> visibleMeshedSections = new ReferenceArrayList<>();
    private final ReferenceArrayList<RenderSection> visibleTickingSections = new ReferenceArrayList<>();
    private final ReferenceArrayList<BlockEntity> visibleBlockEntities = new ReferenceArrayList<>();

    private final Set<BlockEntity> globalBlockEntities = new ObjectOpenHashSet<>();

    private final boolean alwaysDeferChunkUpdates = SodiumClientMod.options().performance.alwaysDeferChunkUpdates;
    private BitArray sectionVisibility;

    public RenderSectionManager(RenderDevice device, SodiumWorldRenderer worldRenderer, ChunkRenderPassManager renderPassManager, ClientWorld world, int chunkViewDistance) {
        var vertexType = createVertexType();

        this.device = device;
        this.chunkRenderers = new Reference2ReferenceOpenHashMap<>();

        this.indexBuffer = new SequenceIndexBuffer(device, SequenceBuilder.QUADS);

        for (var renderPass : DefaultRenderPasses.ALL) {
            this.chunkRenderers.put(renderPass, createChunkRenderer(device, this.indexBuffer, vertexType, renderPass));
        }

        this.world = world;

        this.builder = new ChunkBuilder(vertexType);
        this.builder.init(world, renderPassManager);

        this.needsUpdate = true;
        this.chunkViewDistance = chunkViewDistance;

        this.regions = new RenderRegionManager(device, vertexType);
        this.sectionCache = new ClonedChunkSectionCache(this.world);

        for (ChunkUpdateType type : ChunkUpdateType.values()) {
            this.rebuildQueues.put(type, new ObjectArrayFIFOQueue<>());
        }

        this.tracker = worldRenderer.getChunkTracker();
        this.tree = new ChunkTree(4, RenderSection::new);
    }

    public void reloadChunks(ChunkTracker tracker) {
        tracker.getChunks(ChunkStatus.FLAG_HAS_BLOCK_DATA)
                .forEach(pos -> this.onChunkAdded(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos)));
    }

    public void setFrameIndex(int frame) {
        this.currentFrame = frame;
    }

    public void update(ChunkCameraContext camera, Frustum frustum, boolean spectator) {
        this.camera = camera;

        if (this.renderLists != null) {
            ChunkPrep.deleteRenderLists(this.device, this.renderLists);
        }

        var occlusionResults = ChunkOcclusion.calculateVisibleSections(this.tree, this.camera, frustum, this.world, this.chunkViewDistance, spectator);

        this.sectionVisibility = occlusionResults.visibilityTable();
        this.updateVisibilityLists(occlusionResults.visibleList());

        this.renderLists = ChunkPrep.createRenderLists(this.device, this.regions, this.visibleMeshedSections, this.camera);
        this.needsUpdate = false;
    }

    private void updateVisibilityLists(ReferenceList<RenderSection> visible) {
        for (PriorityQueue<RenderSection> queue : this.rebuildQueues.values()) {
            queue.clear();
        }

        this.visibleBlockEntities.clear();
        this.visibleTickingSections.clear();
        this.visibleMeshedSections.clear();

        for (var section : visible) {
            var data = section.data();

            if (data.meshes != null) {
                this.visibleMeshedSections.add(section);
            }

            if (data.animatedSprites != null) {
                this.visibleTickingSections.add(section);
            }

            if (data.blockEntities != null) {
                for (var entity : data.blockEntities) {
                    this.visibleBlockEntities.add(entity);
                }
            }

            this.schedulePendingUpdates(section);
        }
    }

    private void schedulePendingUpdates(RenderSection section) {
        if (section.getPendingUpdate() == null || !this.tracker.hasMergedFlags(section.getChunkX(), section.getChunkZ(), ChunkStatus.FLAG_ALL)) {
            return;
        }

        PriorityQueue<RenderSection> queue = this.rebuildQueues.get(section.getPendingUpdate());

        if (queue.size() >= 32) {
            return;
        }

        queue.enqueue(section);
    }

    public Iterable<BlockEntity> getVisibleBlockEntities() {
        return this.visibleBlockEntities;
    }

    public Iterable<BlockEntity> getGlobalBlockEntities() {
        return this.globalBlockEntities;
    }

    public void onChunkAdded(int x, int z) {
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.needsUpdate |= this.loadSection(x, y, z);
        }
    }

    public void onChunkRemoved(int x, int z) {
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.needsUpdate |= this.unloadSection(x, y, z);
        }
    }

    private boolean loadSection(int x, int y, int z) {
        var render = this.tree.add(x, y, z);

        Chunk chunk = this.world.getChunk(x, z);
        ChunkSection section = chunk.getSectionArray()[this.world.sectionCoordToIndex(y)];

        if (section.isEmpty()) {
            render.setData(ChunkRenderData.EMPTY);
        } else {
            render.markForUpdate(ChunkUpdateType.INITIAL_BUILD);
        }

        this.onChunkDataChanged(render, ChunkRenderData.ABSENT, render.data());

        return true;
    }

    private boolean unloadSection(int x, int y, int z) {
        RenderSection chunk = this.tree.getSection(x, y, z);

        if (chunk == null) {
            throw new IllegalStateException("Chunk is not loaded: " + ChunkSectionPos.from(x, y, z));
        }

        chunk.delete();

        // TODO: consolidate into get(..) call
        this.tree.remove(x, y, z);

        return true;
    }

    public void renderLayer(ChunkRenderMatrices matrices, ChunkRenderPass renderPass) {
        if (this.renderLists == null || !this.renderLists.containsKey(renderPass)) {
            return;
        }

        var chunkRenderer = this.chunkRenderers.get(renderPass);

        if (chunkRenderer != null) {
            chunkRenderer.render(this.renderLists.get(renderPass), renderPass, matrices, this.currentFrame);
        }
    }

    public void tickVisibleRenders() {
        for (RenderSection render : this.visibleTickingSections) {
            for (Sprite sprite : render.data().animatedSprites) {
                SpriteUtil.markSpriteActive(sprite);
            }
        }
    }

    public boolean isSectionVisible(int x, int y, int z) {
        RenderSection render = this.tree.getSection(x, y, z);

        if (render == null) {
            return false;
        }

        if (this.sectionVisibility == null || this.sectionVisibility.capacity() < render.id()) {
            return false;
        }

        return this.sectionVisibility.get(render.id());
    }

    public void updateChunks() {
        var blockingFutures = this.submitRebuildTasks(ChunkUpdateType.IMPORTANT_REBUILD);

        this.submitRebuildTasks(ChunkUpdateType.INITIAL_BUILD);
        this.submitRebuildTasks(ChunkUpdateType.REBUILD);

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.needsUpdate |= this.performPendingUploads();

        if (!blockingFutures.isEmpty()) {
            this.needsUpdate = true;
            this.regions.uploadChunks(new WorkStealingFutureDrain<>(blockingFutures, this.builder::stealTask), this::onChunkDataChanged);
        }

        this.regions.cleanup();
    }

    private LinkedList<CompletableFuture<TerrainBuildResult>> submitRebuildTasks(ChunkUpdateType filterType) {
        int budget = filterType.isImportant() ? Integer.MAX_VALUE : this.builder.getSchedulingBudget();

        LinkedList<CompletableFuture<TerrainBuildResult>> immediateFutures = new LinkedList<>();
        PriorityQueue<RenderSection> queue = this.rebuildQueues.get(filterType);

        while (budget > 0 && !queue.isEmpty()) {
            RenderSection section = queue.dequeue();

            if (section.isDisposed()) {
                continue;
            }

            // Sections can move between update queues, but they won't be removed from the queue they were
            // previously in to save CPU cycles. We just filter any changed entries here instead.
            if (section.getPendingUpdate() != filterType) {
                continue;
            }

            AbstractBuilderTask task = this.createTerrainBuildTask(section);
            CompletableFuture<?> future;

            if (filterType.isImportant()) {
                CompletableFuture<TerrainBuildResult> immediateFuture = this.builder.schedule(task);
                immediateFutures.add(immediateFuture);

                future = immediateFuture;
            } else {
                future = this.builder.scheduleDeferred(task);
            }

            section.onBuildSubmitted(future);

            budget--;
        }

        return immediateFutures;
    }

    private boolean performPendingUploads() {
        Iterator<TerrainBuildResult> it = this.builder.createDeferredBuildResultDrain();

        if (!it.hasNext()) {
            return false;
        }

        this.regions.uploadChunks(it, this::onChunkDataChanged);

        return true;
    }

    private void onChunkDataChanged(RenderSection section, ChunkRenderData prev, ChunkRenderData next) {
        ListUtil.updateList(this.globalBlockEntities, prev.globalBlockEntities, next.globalBlockEntities);

        var node = this.tree.getNodeById(section.id());

        if (node != null) {
            node.setVisibilityData(next.visibilityData);
        }
    }

    public AbstractBuilderTask createTerrainBuildTask(RenderSection render) {
        WorldSliceData data = WorldSliceData.prepare(this.world, render.getChunkPos(), this.sectionCache);
        int frame = this.currentFrame;

        if (data == null) {
            return new EmptyTerrainBuildTask(render, frame);
        }

        return new TerrainBuildTask(render, data, frame);
    }

    public void markGraphDirty() {
        this.needsUpdate = true;
    }

    public boolean isGraphDirty() {
        return this.needsUpdate;
    }

    public ChunkBuilder getBuilder() {
        return this.builder;
    }

    public void destroy() {
        this.regions.delete();
        this.builder.stopWorkers();

        for (var renderer : this.chunkRenderers.values()) {
            renderer.delete();
        }

        this.chunkRenderers.clear();
        this.indexBuffer.delete();
    }

    public int getTotalSections() {
        return this.tree.getLoadedSections();
    }

    public int getVisibleSectionCount() {
        return this.visibleMeshedSections.size();
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        this.sectionCache.invalidate(x, y, z);

        RenderSection section = this.tree.getSection(x, y, z);

        if (section != null && section.isBuilt()) {
            if (!this.alwaysDeferChunkUpdates && (important || this.isBlockUpdatePrioritized(section))) {
                section.markForUpdate(ChunkUpdateType.IMPORTANT_REBUILD);
            } else {
                section.markForUpdate(ChunkUpdateType.REBUILD);
            }
        }

        this.needsUpdate = true;
    }

    public boolean isBlockUpdatePrioritized(RenderSection render) {
        var camera = this.camera;

        if (camera == null) {
            return false;
        }

        return render.getDistance(camera.posX, camera.posY, camera.posZ) <= NEARBY_BLOCK_UPDATE_DISTANCE;
    }

    public Collection<String> getDebugStrings() {
        int count = 0;

        long deviceUsed = 0;
        long deviceAllocated = 0;

        for (var region : this.regions.getLoadedRegions()) {
            deviceUsed += region.getDeviceUsedMemory();
            deviceAllocated += region.getDeviceAllocatedMemory();

            count++;
        }

        List<String> strings = new ArrayList<>();
        strings.add(String.format("Device buffer objects: %d", count));
        strings.add(String.format("Device memory: %d MiB used/%d MiB alloc", MathUtil.toMib(deviceUsed), MathUtil.toMib(deviceAllocated)));

        return strings;
    }

    private static ChunkRenderer createChunkRenderer(RenderDevice device, SequenceIndexBuffer indexBuffer, TerrainVertexType vertexType, ChunkRenderPass pass) {
        return new DefaultChunkRenderer(device, indexBuffer, vertexType, pass);
    }

    private static TerrainVertexType createVertexType() {
        return SodiumClientMod.options().performance.useCompactVertexFormat ? TerrainVertexFormats.COMPACT : TerrainVertexFormats.STANDARD;
    }
}
