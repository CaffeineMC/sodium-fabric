package net.caffeinemc.sodium.render.chunk;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.util.misc.MathUtil;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.SodiumWorldRenderer;
import net.caffeinemc.sodium.render.chunk.compile.ChunkBuilder;
import net.caffeinemc.sodium.render.chunk.compile.tasks.AbstractBuilderTask;
import net.caffeinemc.sodium.render.chunk.compile.tasks.EmptyTerrainBuildTask;
import net.caffeinemc.sodium.render.chunk.compile.tasks.TerrainBuildResult;
import net.caffeinemc.sodium.render.chunk.compile.tasks.TerrainBuildTask;
import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.caffeinemc.sodium.render.chunk.draw.ChunkRenderMatrices;
import net.caffeinemc.sodium.render.chunk.draw.ChunkRenderer;
import net.caffeinemc.sodium.render.chunk.draw.MdbvChunkRenderer;
import net.caffeinemc.sodium.render.chunk.draw.MdiChunkRenderer;
import net.caffeinemc.sodium.render.chunk.draw.SortedTerrainLists;
import net.caffeinemc.sodium.render.chunk.cull.SectionCuller;
import net.caffeinemc.sodium.render.chunk.cull.SectionTree;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegionManager;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderData;
import net.caffeinemc.sodium.render.chunk.state.ChunkRenderFlag;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexFormats;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.render.texture.SpriteUtil;
import net.caffeinemc.sodium.util.ListUtil;
import net.caffeinemc.sodium.util.tasks.WorkStealingFutureDrain;
import net.caffeinemc.sodium.world.ChunkStatus;
import net.caffeinemc.sodium.world.ChunkTracker;
import net.caffeinemc.sodium.world.slice.WorldSliceData;
import net.caffeinemc.sodium.world.slice.cloned.ClonedChunkSectionCache;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

public class TerrainRenderManager {
    /**
     * The maximum distance a chunk can be from the player's camera in order to be eligible for blocking updates.
     */
    private static final double NEARBY_BLOCK_UPDATE_DISTANCE = 32.0;
    private static final int MAX_REBUILDS_PER_RENDERER_UPDATE = 32;
    
    private final SortedTerrainLists sortedTerrainLists;

    private final ChunkBuilder builder;

    private final RenderRegionManager regionManager;
    private final ClonedChunkSectionCache sectionCache;

    private final SectionTree sectionTree;
    private final SectionCuller sectionCuller;
    
    private final Map<ChunkUpdateType, PriorityQueue<RenderSection>> rebuildQueues = new EnumMap<>(ChunkUpdateType.class);

    private final ChunkRenderer chunkRenderer;

    private final ClientWorld world;

    private boolean needsUpdate = true;
    private int frameIndex = 0;

    private final ChunkTracker tracker;

    private final ChunkCameraContext camera;

    private final List<RenderSection> visibleMeshedSections = new ReferenceArrayList<>();
    private final List<RenderSection> visibleTickingSections = new ReferenceArrayList<>();
    private final List<RenderSection> visibleBlockEntitySections = new ReferenceArrayList<>();

    private final Set<BlockEntity> globalBlockEntities = new ObjectOpenHashSet<>();

    private final boolean alwaysDeferChunkUpdates = SodiumClientMod.options().performance.alwaysDeferChunkUpdates;
    
//    private final ChunkGeometrySorter chunkGeometrySorter;

    public TerrainRenderManager(
            RenderDevice device,
            SodiumWorldRenderer worldRenderer,
            ChunkRenderPassManager renderPassManager,
            ClientWorld world,
            ChunkCameraContext camera,
            int chunkViewDistance
    ) {
        TerrainVertexType vertexType = createVertexType();
    
        this.world = world;
        this.camera = camera;
    
        this.chunkRenderer = createChunkRenderer(device, camera, renderPassManager, vertexType);

        this.builder = new ChunkBuilder(vertexType);
        this.builder.init(world, renderPassManager);
    
        this.regionManager = new RenderRegionManager(device, vertexType);
        this.sectionCache = new ClonedChunkSectionCache(this.world);
    
        this.sortedTerrainLists = new SortedTerrainLists(this.regionManager, renderPassManager, camera);

        for (ChunkUpdateType type : ChunkUpdateType.values()) {
            this.rebuildQueues.put(type, new ObjectArrayFIFOQueue<>());
        }

        this.tracker = worldRenderer.getChunkTracker();
        this.sectionTree = new SectionTree(
                3,
                chunkViewDistance + 3,
                world,
                camera
        );
        this.sectionCuller = new SectionCuller(this.sectionTree, chunkViewDistance);
        
        // TODO: uncomment when working on translucency sorting
//        if (SodiumClientMod.options().quality.useTranslucentFaceSorting) {
//            this.chunkGeometrySorter = new ChunkGeometrySorter(device, renderPassManager, vertexType, (float) Math.toRadians(5.0f));
//        } else {
//            this.chunkGeometrySorter = null;
//        }
    }

    public void reloadChunks(ChunkTracker tracker) {
        tracker.getChunks(ChunkStatus.FLAG_HAS_BLOCK_DATA)
                .forEach(pos -> this.onChunkAdded(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos)));
    }

    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }

    public void update(Frustum frustum, boolean spectator) {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();
    
        profiler.swap("chunk_graph_rebuild");
        BlockPos origin = this.camera.getBlockPos();
        var useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled &&
                (!spectator || !this.world.getBlockState(origin).isOpaqueFullCube(this.world, origin));

        this.sectionCuller.calculateVisibleSections(
                frustum,
                useOcclusionCulling
        );

        this.updateVisibilityLists();
    
//        if (this.chunkGeometrySorter != null) {
//            profiler.swap("translucency_sort");
//            this.chunkGeometrySorter.sortGeometry(this.visibleMeshedSections, camera);
//        }

        profiler.swap("create_render_lists");
        this.sortedTerrainLists.update(this.visibleMeshedSections);
        this.chunkRenderer.createRenderLists(this.sortedTerrainLists, this.frameIndex);
        
        this.needsUpdate = false;
    }

    private void updateVisibilityLists() {
        for (PriorityQueue<RenderSection> queue : this.rebuildQueues.values()) {
            queue.clear();
        }

        this.visibleMeshedSections.clear();
        this.visibleTickingSections.clear();
        this.visibleBlockEntitySections.clear();
        
        Iterator<RenderSection> sectionItr = this.sectionCuller.getVisibleSectionIterator();
    
        while (sectionItr.hasNext()) {
            RenderSection section = sectionItr.next();

            if (section == null) {
                continue;
            }

            if (section.getPendingUpdate() != null) {
                this.schedulePendingUpdates(section);
            }

            int flags = section.getFlags();

            if (ChunkRenderFlag.has(flags, ChunkRenderFlag.HAS_TERRAIN_MODELS)) {
                this.visibleMeshedSections.add(section);
            }

            if (ChunkRenderFlag.has(flags, ChunkRenderFlag.HAS_TICKING_TEXTURES)) {
                this.visibleTickingSections.add(section);
            }

            if (ChunkRenderFlag.has(flags, ChunkRenderFlag.HAS_BLOCK_ENTITIES)) {
                this.visibleBlockEntitySections.add(section);
            }
        }
    }

    private void schedulePendingUpdates(RenderSection section) {
        PriorityQueue<RenderSection> queue = this.rebuildQueues.get(section.getPendingUpdate());

        if (queue.size() < MAX_REBUILDS_PER_RENDERER_UPDATE && this.tracker.hasMergedFlags(section.getSectionX(), section.getSectionZ(), ChunkStatus.FLAG_ALL)) {
            queue.enqueue(section);
        }
    }

    public Iterable<BlockEntity> getVisibleBlockEntities() {
        return () -> this.visibleBlockEntitySections.stream()
                .flatMap(section -> Arrays.stream(section.getData().blockEntities))
                .iterator();
    }

    public Iterable<BlockEntity> getGlobalBlockEntities() {
        return this.globalBlockEntities;
    }

    public void onChunkAdded(int x, int z) {
        // we can't check the visibility bit here, but we can check if it's in draw distance, which effectively does
        // fog culling on updates.
        boolean inDrawDistance = this.sectionCuller.isChunkInDrawDistance(x, z);
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.needsUpdate |= this.loadSection(x, y, z) && inDrawDistance;
        }
    }

    public void onChunkRemoved(int x, int z) {
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.needsUpdate |= this.unloadSection(x, y, z)
                                && this.sectionCuller.isSectionVisible(x, y, z);
        }
    }

    private boolean loadSection(int x, int y, int z) {
        RenderSection renderSection = this.sectionTree.add(x, y, z);

        Chunk chunk = this.world.getChunk(x, z);
        ChunkSection section = chunk.getSectionArray()[this.world.sectionCoordToIndex(y)];
        
        if (section.isEmpty()) {
            renderSection.setData(ChunkRenderData.EMPTY);
        } else {
            renderSection.markForUpdate(ChunkUpdateType.INITIAL_BUILD);
        }
        
        this.onChunkDataChanged(x, y, z, ChunkRenderData.ABSENT, renderSection.getData());
        
        return true;
    }

    private boolean unloadSection(int x, int y, int z) {
        RenderSection section = this.sectionTree.remove(x, y, z);
        
        if (section != null) {
//            if (this.chunkGeometrySorter != null) {
//                this.chunkGeometrySorter.removeSection(section);
//            }
            section.delete();
            return true;
        } else {
            return false;
        }
    }

    public void renderLayer(ChunkRenderMatrices matrices, ChunkRenderPass renderPass) {
        this.chunkRenderer.render(renderPass, matrices, this.frameIndex);
    }

    public void tickVisibleRenders() {
        for (RenderSection render : this.visibleTickingSections) {
            for (Sprite sprite : render.getData().animatedSprites) {
                SpriteUtil.markSpriteActive(sprite);
            }
        }
    }

    public boolean isSectionVisible(int x, int y, int z) {
        return this.sectionCuller.isSectionVisible(x, y, z);
    }

    public void updateChunks() {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();
        
        var blockingFutures = this.submitRebuildTasks(ChunkUpdateType.IMPORTANT_REBUILD);

        this.submitRebuildTasks(ChunkUpdateType.INITIAL_BUILD);
        this.submitRebuildTasks(ChunkUpdateType.REBUILD);

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.needsUpdate |= this.performPendingUploads();

        if (!blockingFutures.isEmpty()) {
            this.needsUpdate = true;
            this.regionManager.uploadChunks(
                    new WorkStealingFutureDrain<>(
                            blockingFutures,
                            this.builder::stealTask
                    ),
                    this.frameIndex,
                    this::onChunkDataChanged
            );
        }
    
        profiler.swap("chunk_cleanup");
        this.regionManager.cleanup();
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

        this.regionManager.uploadChunks(it, this.frameIndex, this::onChunkDataChanged);

        return true;
    }

    private void onChunkDataChanged(int x, int y, int z, ChunkRenderData prev, ChunkRenderData next) {
        ListUtil.updateList(this.globalBlockEntities, prev.globalBlockEntities, next.globalBlockEntities);

        this.sectionCuller.setVisibilityData(x, y, z, next.occlusionData);
    }

    public AbstractBuilderTask createTerrainBuildTask(RenderSection render) {
        WorldSliceData data = WorldSliceData.prepare(this.world, render.getChunkPos(), this.sectionCache);
        int frame = this.frameIndex;

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
        this.regionManager.delete();
        this.builder.stopWorkers();
        this.chunkRenderer.delete();
//        if (this.chunkGeometrySorter != null) {
//            this.chunkGeometrySorter.delete();
//        }
    }

    public int getTotalSections() {
        return this.sectionTree.getLoadedSections();
    }

    public int getVisibleSectionCount() {
        return this.visibleMeshedSections.size();
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        this.sectionCache.invalidate(x, y, z);

        RenderSection section = this.sectionTree.getSection(x, y, z);

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
        if (!this.camera.isCameraInitialized()) {
            return false;
        }
    
        Vec3d cameraPos = this.camera.getPos();
        return render.getDistanceSq(cameraPos.getX(), cameraPos.getY(), cameraPos.getZ()) <= NEARBY_BLOCK_UPDATE_DISTANCE;
    }

    public Collection<String> getDebugStrings() {
        int count = 0;

        long deviceUsed = 0;
        long deviceAllocated = 0;
        
        deviceAllocated += this.regionManager.getDeviceAllocatedMemory();
        deviceUsed += this.regionManager.getDeviceUsedMemory();
        count += this.regionManager.getDeviceBufferObjects();

        deviceUsed += this.chunkRenderer.getDeviceUsedMemory();
        deviceAllocated += this.chunkRenderer.getDeviceAllocatedMemory();
        count += this.chunkRenderer.getDeviceBufferObjects();

        List<String> strings = new ArrayList<>();
        strings.add(String.format("Chunk Renderer: %s", this.chunkRenderer.getDebugName()));
        strings.add(String.format("Device buffer objects: %d", count));
        strings.add(String.format("Device memory: %d MiB used/%d MiB alloc", MathUtil.toMib(deviceUsed), MathUtil.toMib(deviceAllocated)));

        return strings;
    }

    private static ChunkRenderer createChunkRenderer(
            RenderDevice device,
            ChunkCameraContext camera,
            ChunkRenderPassManager renderPassManager,
            TerrainVertexType vertexType
    ) {
        return switch (SodiumClientMod.options().advanced.chunkRendererBackend) {
            case DEFAULT -> device.properties().preferences.directRendering
                            ? new MdbvChunkRenderer(device, camera, renderPassManager, vertexType)
                            : new MdiChunkRenderer(device, camera, renderPassManager, vertexType);
            
            case BASEVERTEX -> new MdbvChunkRenderer(device, camera, renderPassManager, vertexType);
            
            case INDIRECT -> new MdiChunkRenderer(device, camera, renderPassManager, vertexType);
        };
    }

    private static TerrainVertexType createVertexType() {
        return SodiumClientMod.options().performance.useCompactVertexFormat ? TerrainVertexFormats.COMPACT : TerrainVertexFormats.STANDARD;
    }
}
