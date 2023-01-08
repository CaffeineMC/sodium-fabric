package net.caffeinemc.sodium.render.chunk;

import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.caffeinemc.gfx.api.device.RenderDevice;
import net.caffeinemc.gfx.util.misc.MathUtil;
import net.caffeinemc.sodium.SodiumClientMod;
import net.caffeinemc.sodium.interop.vanilla.math.frustum.Frustum;
import net.caffeinemc.sodium.render.chunk.compile.ChunkBuilder;
import net.caffeinemc.sodium.render.chunk.compile.tasks.AbstractBuilderTask;
import net.caffeinemc.sodium.render.chunk.compile.tasks.EmptyTerrainBuildTask;
import net.caffeinemc.sodium.render.chunk.compile.tasks.SectionBuildResult;
import net.caffeinemc.sodium.render.chunk.compile.tasks.TerrainBuildTask;
import net.caffeinemc.sodium.render.chunk.cull.SectionCuller;
import net.caffeinemc.sodium.render.chunk.draw.ChunkCameraContext;
import net.caffeinemc.sodium.render.chunk.draw.ChunkRenderMatrices;
import net.caffeinemc.sodium.render.chunk.draw.ChunkRenderer;
import net.caffeinemc.sodium.render.chunk.draw.MdbvChunkRenderer;
import net.caffeinemc.sodium.render.chunk.draw.MdiChunkRenderer;
import net.caffeinemc.sodium.render.chunk.draw.SortedTerrainLists;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPass;
import net.caffeinemc.sodium.render.chunk.passes.ChunkRenderPassManager;
import net.caffeinemc.sodium.render.chunk.region.RenderRegionManager;
import net.caffeinemc.sodium.render.chunk.state.SectionRenderData;
import net.caffeinemc.sodium.render.chunk.state.SectionRenderFlags;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexFormats;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.render.texture.SpriteUtil;
import net.caffeinemc.sodium.util.tasks.WorkStealingFutureDrain;
import net.caffeinemc.sodium.world.ChunkTracker;
import net.caffeinemc.sodium.world.slice.WorldSliceData;
import net.caffeinemc.sodium.world.slice.cloned.ClonedChunkSectionCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.chunk.ChunkSection;

public class TerrainRenderManager {
    
    private final SortedTerrainLists sortedTerrainLists;
    private final SortedSectionLists sortedSectionLists;
    
    private final ChunkBuilder builder;
    
    private final BlockEntityRenderManager blockEntityRenderManager;
    
    private final RenderRegionManager regionManager;
    private final ClonedChunkSectionCache sectionCache;
    
    private final SectionTree sectionTree;
    private final SectionCuller sectionCuller;
    
    private final ChunkRenderer chunkRenderer;
    
    private final ClientWorld world;
    
    private boolean needsUpdate = true;
    private int frameIndex = 0;
    
    private final ChunkTracker chunkTracker;
    private final int chunkViewDistance;
    private ChunkTracker.Area watchedArea;
    
    private final ChunkCameraContext camera;
    
//    private final ChunkGeometrySorter chunkGeometrySorter;
    
    private final boolean alwaysDeferChunkUpdates = SodiumClientMod.options().performance.alwaysDeferChunkUpdates;

    public TerrainRenderManager(
            RenderDevice device,
            ChunkTracker chunkTracker,
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

        this.chunkTracker = chunkTracker;
        this.chunkViewDistance = chunkViewDistance;
        
        this.sectionTree = new SectionTree(
                3,
                ChunkTracker.getChunkMapRadius(chunkViewDistance + 3),
                world,
                camera
        );
        
        this.sortedSectionLists = new SortedSectionLists(this.sectionTree);
    
        this.sortedTerrainLists = new SortedTerrainLists(
                this.regionManager,
                renderPassManager,
                this.sectionTree,
                this.sortedSectionLists,
                camera
        );
        
        this.blockEntityRenderManager = new BlockEntityRenderManager(this.sectionTree, this.sortedSectionLists);
        
        this.sectionCuller = new SectionCuller(this.sectionTree, this.sortedSectionLists, chunkViewDistance);
    
        // TODO: uncomment when working on translucency sorting
//        if (SodiumClientMod.options().quality.useTranslucentFaceSorting) {
//            this.chunkGeometrySorter = new ChunkGeometrySorter(device, renderPassManager, vertexType, (float) Math.toRadians(5.0f));
//        } else {
//            this.chunkGeometrySorter = null;
//        }
    }
    
    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }

    public void update(Frustum frustum, boolean spectator) {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();
    
        this.updateWatchedArea();
        this.processChunkQueues();
    
        profiler.swap("update_section_lists");
        BlockPos origin = this.camera.getBlockPos();
        var useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled &&
                (!spectator || !this.world.getBlockState(origin).isOpaqueFullCube(this.world, origin));
        
        this.sortedSectionLists.reset();
        this.sectionCuller.calculateVisibleSections(
                frustum,
                useOcclusionCulling
        );
    
        this.blockEntityRenderManager.update();
    
//        if (this.chunkGeometrySorter != null) {
//            profiler.swap("translucency_sort");
//            this.chunkGeometrySorter.sortGeometry(this.visibleMeshedSections, camera);
//        }

        profiler.swap("update_render_lists");
        this.sortedTerrainLists.update();
        this.chunkRenderer.createRenderLists(this.sortedTerrainLists, this.frameIndex);
        
        this.needsUpdate = false;
    }
    
    private void updateWatchedArea() {
        int x = this.camera.getSectionX();
        int z = this.camera.getSectionZ();
        
        var prevArea = this.watchedArea;
        var newArea = new ChunkTracker.Area(x, z, this.chunkViewDistance);
        
        if (Objects.equals(prevArea, newArea)) {
            return;
        }
        
        if (prevArea != null) {
            this.chunkTracker.updateWatchedArea(prevArea, newArea);
        } else {
            this.chunkTracker.addWatchedArea(newArea);
        }
        
        this.watchedArea = newArea;
    }
    
    public void processChunkQueues() {
        var queue = this.chunkTracker.getEvents(this.watchedArea);
        
        for (LongIterator iterator = queue.removed().iterator(); iterator.hasNext(); ) {
            long pos = iterator.nextLong();
            
            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);
            
            for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
                this.unloadSection(x, y, z);
            }
        }
        
        for (LongIterator iterator = queue.added().iterator(); iterator.hasNext(); ) {
            long pos = iterator.nextLong();
            
            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);
            
            for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
                this.loadSection(x, y, z);
            }
        }
    }
    
    private void loadSection(int x, int y, int z) {
        int sectionIdx = this.sectionTree.getSectionIdxUnchecked(x, y, z);
        
        RenderSection renderSection = this.sectionTree.add(x, y, z);
        ChunkSection section = this.world.getChunk(x, z).getSectionArray()[this.world.sectionCoordToIndex(y)];
        
        if (section.isEmpty()) {
            renderSection.setData(SectionRenderData.EMPTY);
        } else {
            this.sectionTree.addSectionFlags(sectionIdx, SectionRenderFlags.NEEDS_UPDATE);
            this.sectionTree.removeSectionFlags(sectionIdx, SectionRenderFlags.NEEDS_UPDATE_REBUILD | SectionRenderFlags.NEEDS_UPDATE_IMPORTANT);
        }
        
        SectionRenderData data = renderSection.getData();
        
        this.blockEntityRenderManager.addGlobalBlockEntities(data);
        this.sectionCuller.setVisibilityData(sectionIdx, data.occlusionData);
        
        // clear existing flags and add default content flags
        this.sectionTree.removeSectionFlags(sectionIdx, SectionRenderFlags.ALL_CONTENT_FLAGS);
        this.sectionTree.addSectionFlags(sectionIdx, data.getBaseFlags());
    }
    
    private void unloadSection(int x, int y, int z) {
        RenderSection section = this.sectionTree.remove(x, y, z);
        
        if (section != null) {
//            if (this.chunkGeometrySorter != null) {
//                this.chunkGeometrySorter.removeSection(section);
//            }
            section.delete();
        }
    }
    
    public void renderLayer(ChunkRenderMatrices matrices, ChunkRenderPass renderPass) {
        this.chunkRenderer.render(renderPass, matrices, this.frameIndex);
    }
    
    public void tickVisibleRenders() {
        for (int i = 0; i < this.sortedSectionLists.tickingTextureSectionCount; i++) {
            int sectionIdx = this.sortedSectionLists.tickingTextureSectionIdxs[i];
            RenderSection section = this.sectionTree.getSection(sectionIdx);
            
            for (Sprite sprite : section.getData().animatedSprites) {
                SpriteUtil.markSpriteActive(sprite);
            }
        }
    }
    
    public boolean isSectionVisible(int x, int y, int z) {
        return this.sectionCuller.isSectionVisible(x, y, z);
    }
    
    public void updateChunks() {
        Profiler profiler = MinecraftClient.getInstance().getProfiler();
        
        var blockingFutures = this.submitBuildTasks();

        // Try to complete some other work on the main thread while we wait for builds to complete
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
    
    private LinkedList<CompletableFuture<SectionBuildResult>> submitBuildTasks() {
        int budget = this.builder.getSchedulingBudget();

        LinkedList<CompletableFuture<SectionBuildResult>> immediateFutures = new LinkedList<>();
        
        for (int i = 0; i < this.sortedSectionLists.importantUpdateSectionCount; i++) {
            int sectionIdx = this.sortedSectionLists.importantUpdatableSectionIdxs[i];
            if (sectionIdx == SectionTree.NULL_INDEX) {
                continue;
            }
            
            RenderSection section = this.sectionTree.getSection(sectionIdx);
    
            // remove early, reset flags, and skip
            if (section.isDisposed()) {
                this.sortedSectionLists.importantUpdatableSectionIdxs[i] = SectionTree.NULL_INDEX;
                this.sectionTree.removeSectionFlags(sectionIdx, SectionRenderFlags.ALL_UPDATE_FLAGS);
                continue;
            }
    
            // Sections can move between update queues, but they won't be removed from the queue they were
            // previously in to save CPU cycles. We just filter any changed entries here instead.
            // TODO: is this still needed?
            // remove early but don't reset flags, then skip
            if (!this.sectionTree.sectionHasAllFlags(sectionIdx, SectionRenderFlags.NEEDS_UPDATE | SectionRenderFlags.NEEDS_UPDATE_IMPORTANT)) {
                this.sortedSectionLists.importantUpdatableSectionIdxs[i] = SectionTree.NULL_INDEX;
                continue;
            }
    
            AbstractBuilderTask task = this.createTerrainBuildTask(section);
            CompletableFuture<?> future;
    
            CompletableFuture<SectionBuildResult> immediateFuture = this.builder.schedule(task);
            immediateFutures.add(immediateFuture);
    
            future = immediateFuture;
    
            section.onBuildSubmitted(future);
    
            budget--;
            
            // remove from queue once processed
            this.sortedSectionLists.importantUpdatableSectionIdxs[i] = SectionTree.NULL_INDEX;
            this.sectionTree.removeSectionFlags(sectionIdx, SectionRenderFlags.ALL_UPDATE_FLAGS);
        }
    
        for (int i = 0; i < this.sortedSectionLists.secondaryUpdateSectionCount; i++) {
            if (budget <= 0) {
                break;
            }
            
            int sectionIdx = this.sortedSectionLists.secondaryUpdatableSectionIdxs[i];
            if (sectionIdx == SectionTree.NULL_INDEX) {
                continue;
            }
        
            RenderSection section = this.sectionTree.getSection(sectionIdx);
    
            // remove early, reset flags, and skip
            if (section.isDisposed()) {
                this.sortedSectionLists.secondaryUpdatableSectionIdxs[i] = SectionTree.NULL_INDEX;
                this.sectionTree.removeSectionFlags(sectionIdx, SectionRenderFlags.ALL_UPDATE_FLAGS);
                continue;
            }
    
            // Sections can move between update queues, but they won't be removed from the queue they were
            // previously in to save CPU cycles. We just filter any changed entries here instead.
            // TODO: is this still needed?
            // remove early but don't reset flags, then skip
            byte flagData = this.sectionTree.getSectionFlags(sectionIdx);
            if (!SectionRenderFlags.hasAny(flagData, SectionRenderFlags.NEEDS_UPDATE)
                || SectionRenderFlags.hasAny(flagData, SectionRenderFlags.NEEDS_UPDATE_IMPORTANT)) {
                this.sortedSectionLists.secondaryUpdatableSectionIdxs[i] = SectionTree.NULL_INDEX;
                continue;
            }

            AbstractBuilderTask task = this.createTerrainBuildTask(section);
            CompletableFuture<?> future = this.builder.scheduleDeferred(task);
            section.onBuildSubmitted(future);

            budget--;
    
            // remove from queue once processed
            this.sortedSectionLists.secondaryUpdatableSectionIdxs[i] = SectionTree.NULL_INDEX;
            this.sectionTree.removeSectionFlags(sectionIdx, SectionRenderFlags.ALL_UPDATE_FLAGS);
        }

        return immediateFutures;
    }
    
    private boolean performPendingUploads() {
        Iterator<SectionBuildResult> it = this.builder.createDeferredBuildResultDrain();

        if (!it.hasNext()) {
            return false;
        }

        this.regionManager.uploadChunks(it, this.frameIndex, this::onChunkDataChanged);

        return true;
    }
    
    private void onChunkDataChanged(RenderSection section, SectionBuildResult result) {
        SectionRenderData prev = section.getData();
        SectionRenderData next = result.data();
    
        int sectionIdx = this.sectionTree.getSectionIdxUnchecked(
                section.getSectionX(),
                section.getSectionY(),
                section.getSectionZ()
        );
        
        this.blockEntityRenderManager.updateGlobalBlockEntities(prev, next);
        this.sectionCuller.setVisibilityData(sectionIdx, next.occlusionData);
    
        section.setData(next);
        section.setLastAcceptedBuildTime(result.buildTime());
        
        this.sectionTree.addSectionFlags(sectionIdx, next.getBaseFlags());
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
    
    public BlockEntityRenderManager getBlockEntityRenderManager() {
        return this.blockEntityRenderManager;
    }
    
    public void destroy() {
        this.regionManager.delete();
        this.builder.stopWorkers();
        this.chunkRenderer.delete();
//        if (this.chunkGeometrySorter != null) {
//            this.chunkGeometrySorter.delete();
//        }
    
        if (this.watchedArea != null) {
            this.chunkTracker.removeWatchedArea(this.watchedArea);
        }
    }

    public int getTotalSections() {
        return this.sectionTree.getTotalLoadedSections();
    }

    public int getVisibleSectionCount() {
        // true representation would probably be:
//        return this.sortedTerrainLists.getFinalSectionCount() + this.sortedSectionLists.blockEntitySectionsCount;
        return this.sortedSectionLists.terrainSectionCount;
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        this.sectionCache.invalidate(x, y, z);

        RenderSection section = this.sectionTree.getSection(x, y, z);

        if (section != null && section.isBuilt()) {
            int sectionIdx = this.sectionTree.getSectionIdxUnchecked(x, y, z);
            
            if (!this.alwaysDeferChunkUpdates && important) {
                this.sectionTree.addSectionFlags(
                        sectionIdx,
                        SectionRenderFlags.NEEDS_UPDATE
                        | SectionRenderFlags.NEEDS_UPDATE_REBUILD
                        | SectionRenderFlags.NEEDS_UPDATE_IMPORTANT
                );
            } else {
                this.sectionTree.addSectionFlags(
                        sectionIdx,
                        SectionRenderFlags.NEEDS_UPDATE
                        | SectionRenderFlags.NEEDS_UPDATE_REBUILD
                );
                this.sectionTree.removeSectionFlags(
                        sectionIdx,
                        SectionRenderFlags.NEEDS_UPDATE_IMPORTANT
                );
            }
        }

        this.needsUpdate = true;
    }
    
    public boolean isSectionReady(int x, int y, int z) {
        var section = this.sectionTree.getSection(x, y, z);
        
        if (section != null) {
            return section.getData() != SectionRenderData.ABSENT;
        }
        
        return false;
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
