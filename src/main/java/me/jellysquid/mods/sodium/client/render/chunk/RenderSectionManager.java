package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMaps;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions.DeferSortMode;
import me.jellysquid.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkSortOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkJobResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkJobCollector;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderSortingTask;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.GraphDirection;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.NoData;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TopoSortDynamicData;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.data.TranslucentData;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger.CameraMovement;
import me.jellysquid.mods.sodium.client.render.chunk.translucent_sorting.trigger.SortTriggering;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.client.render.util.RenderAsserts;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3dc;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RenderSectionManager {
    private final ChunkBuilder builder;

    private final RenderRegionManager regions;
    private final ClonedChunkSectionCache sectionCache;

    private final Long2ReferenceMap<RenderSection> sectionByPosition = new Long2ReferenceOpenHashMap<>();

    private final ConcurrentLinkedDeque<ChunkJobResult<? extends BuilderTaskOutput>> buildResults = new ConcurrentLinkedDeque<>();

    private final ChunkRenderer chunkRenderer;

    private final ClientWorld world;

    private final ReferenceSet<RenderSection> sectionsWithGlobalEntities = new ReferenceOpenHashSet<>();

    private final OcclusionCuller occlusionCuller;

    private final int renderDistance;

    private final SortTriggering ts;

    private ChunkJobCollector lastBlockingCollector;

    @NotNull
    private SortedRenderLists renderLists;

    @NotNull
    private Map<ChunkUpdateType, ArrayDeque<RenderSection>> taskLists;

    private int lastUpdatedFrame;

    private boolean needsGraphUpdate;

    private @Nullable BlockPos cameraBlockPos;
    private @Nullable Vector3dc cameraPosition;

    public RenderSectionManager(ClientWorld world, int renderDistance, CommandList commandList) {
        this.chunkRenderer = new DefaultChunkRenderer(RenderDevice.INSTANCE, ChunkMeshFormats.COMPACT);

        this.world = world;

        this.builder = new ChunkBuilder(world, ChunkMeshFormats.COMPACT);

        this.needsGraphUpdate = true;
        this.renderDistance = renderDistance;

        this.ts = new SortTriggering();

        this.regions = new RenderRegionManager(commandList);
        this.sectionCache = new ClonedChunkSectionCache(this.world);

        this.renderLists = SortedRenderLists.empty();
        this.occlusionCuller = new OcclusionCuller(Long2ReferenceMaps.unmodifiable(this.sectionByPosition), this.world);

        this.taskLists = new EnumMap<>(ChunkUpdateType.class);

        for (var type : ChunkUpdateType.values()) {
            this.taskLists.put(type, new ArrayDeque<>());
        }
    }

    public void updateCameraState(Vector3dc cameraPosition, Camera camera) {
        this.cameraBlockPos = camera.getBlockPos();
        this.cameraPosition = cameraPosition;
    }

    public void update(Camera camera, Viewport viewport, int frame, boolean spectator) {
        this.createTerrainRenderList(camera, viewport, frame, spectator);

        this.needsGraphUpdate = false;
        this.lastUpdatedFrame = frame;
    }

    private void createTerrainRenderList(Camera camera, Viewport viewport, int frame, boolean spectator) {
        this.resetRenderLists();

        final var searchDistance = this.getSearchDistance();
        final var useOcclusionCulling = this.shouldUseOcclusionCulling(camera, spectator);

        var visitor = new VisibleChunkCollector(frame);

        this.occlusionCuller.findVisible(visitor, viewport, searchDistance, useOcclusionCulling, frame);

        this.renderLists = visitor.createRenderLists();
        this.taskLists = visitor.getRebuildLists();
    }

    private float getSearchDistance() {
        float distance;

        if (SodiumClientMod.options().performance.useFogOcclusion) {
            distance = this.getEffectiveRenderDistance();
        } else {
            distance = this.getRenderDistance();
        }

        return distance;
    }

    private boolean shouldUseOcclusionCulling(Camera camera, boolean spectator) {
        final boolean useOcclusionCulling;
        BlockPos origin = camera.getBlockPos();

        if (spectator && this.world.getBlockState(origin)
                .isOpaqueFullCube(this.world, origin))
        {
            useOcclusionCulling = false;
        } else {
            useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled;
        }
        return useOcclusionCulling;
    }

    private void resetRenderLists() {
        this.renderLists = SortedRenderLists.empty();

        for (var list : this.taskLists.values()) {
            list.clear();
        }
    }

    public void onSectionAdded(int x, int y, int z) {
        long key = ChunkSectionPos.asLong(x, y, z);

        if (this.sectionByPosition.containsKey(key)) {
            return;
        }

        RenderRegion region = this.regions.createForChunk(x, y, z);

        RenderSection renderSection = new RenderSection(region, x, y, z);
        region.addSection(renderSection);

        this.sectionByPosition.put(key, renderSection);

        Chunk chunk = this.world.getChunk(x, z);
        ChunkSection section = chunk.getSectionArray()[this.world.sectionCoordToIndex(y)];

        if (section.isEmpty()) {
            this.updateSectionInfo(renderSection, BuiltSectionInfo.EMPTY);
        } else {
            renderSection.setPendingUpdate(ChunkUpdateType.INITIAL_BUILD);
        }

        this.connectNeighborNodes(renderSection);

        this.needsGraphUpdate = true;
    }

    public void onSectionRemoved(int x, int y, int z) {
        long sectionPos = ChunkSectionPos.asLong(x, y, z);
        RenderSection section = this.sectionByPosition.remove(sectionPos);

        if (section == null) {
            return;
        }

        if (section.getTranslucentData() != null) {
            this.ts.removeSection(section.getTranslucentData(), sectionPos);
        }

        RenderRegion region = section.getRegion();

        if (region != null) {
            region.removeSection(section);
        }

        this.disconnectNeighborNodes(section);
        this.updateSectionInfo(section, null);

        section.delete();

        this.needsGraphUpdate = true;
    }

    public void renderLayer(ChunkRenderMatrices matrices, TerrainRenderPass pass, double x, double y, double z) {
        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrices, commandList, this.renderLists, pass, new CameraTransform(x, y, z));

        commandList.flush();
    }

    public void tickVisibleRenders() {
        Iterator<ChunkRenderList> it = this.renderLists.iterator();

        while (it.hasNext()) {
            ChunkRenderList renderList = it.next();

            var region = renderList.getRegion();
            var iterator = renderList.sectionsWithSpritesIterator();

            if (iterator == null) {
                continue;
            }

            while (iterator.hasNext()) {
                var section = region.getSection(iterator.nextByteAsInt());

                if (section == null) {
                    continue;
                }

                var sprites = section.getAnimatedSprites();

                if (sprites == null) {
                    continue;
                }

                for (Sprite sprite : sprites) {
                    SpriteUtil.markSpriteActive(sprite);
                }
            }
        }
    }

    public boolean isSectionVisible(int x, int y, int z) {
        RenderSection render = this.getRenderSection(x, y, z);

        if (render == null) {
            return false;
        }

        return render.getLastVisibleFrame() == this.lastUpdatedFrame;
    }

    public void uploadChunks() {
        var results = this.collectChunkBuildResults();

        if (results.isEmpty()) {
            return;
        }

        this.processChunkBuildResults(results);

        for (var result : results) {
            result.deleteAfterUploadSafe();
        }

        // TODO: only needed if the tasks actually changed the visibility (sort tasks
        // don't count, though there would never be a sort task without camera movement
        // so it likely doesn't matter)
        this.needsGraphUpdate = true;
    }

    private void processChunkBuildResults(ArrayList<BuilderTaskOutput> results) {
        var filtered = filterChunkBuildResults(results);

        this.regions.uploadResults(RenderDevice.INSTANCE.createCommandList(), filtered);

        for (var result : filtered) {
            TranslucentData oldData = result.render.getTranslucentData();
            if (result instanceof ChunkBuildOutput chunkBuildOutput) {
                this.updateSectionInfo(result.render, chunkBuildOutput.info);
                if (chunkBuildOutput.translucentData != null) {
                    this.ts.integrateTranslucentData(oldData, chunkBuildOutput.translucentData, this.cameraPosition);

                    // a rebuild always generates new translucent data which means applyTriggerChanges isn't necessary
                    result.render.setTranslucentData(chunkBuildOutput.translucentData);
                }
            } else if (result instanceof ChunkSortOutput chunkSortOutput 
                    && chunkSortOutput.dynamicData instanceof TopoSortDynamicData data) {
                this.ts.applyTriggerChanges(data, result.render.getPosition(), this.cameraPosition);
            }

            var job = result.render.getTaskCancellationToken();

            // clear the cancellation token (thereby marking the section as not having an
            // active task) if this job is the most recent submitted job for this section
            if (job != null && result.submitTime >= result.render.getLastSubmittedFrame()) {
                result.render.setTaskCancellationToken(null);
            }

            result.render.setLastUploadFrame(result.submitTime);
        }
    }

    private void updateSectionInfo(RenderSection render, BuiltSectionInfo info) {
        render.setInfo(info);

        if (info == null || ArrayUtils.isEmpty(info.globalBlockEntities)) {
            this.sectionsWithGlobalEntities.remove(render);
        } else {
            this.sectionsWithGlobalEntities.add(render);
        }
    }

    private static List<BuilderTaskOutput> filterChunkBuildResults(ArrayList<BuilderTaskOutput> outputs) {
        var map = new Reference2ReferenceLinkedOpenHashMap<RenderSection, BuilderTaskOutput>();

        for (var output : outputs) {
            // when outdated or duplicate outputs are thrown out, make sure to delete their
            // buffers to avoid memory leaks
            if (output.render.isDisposed() || output.render.getLastUploadFrame() > output.submitTime) {
                output.deleteFully();
                continue;
            }

            var render = output.render;
            var previous = map.get(render);

            if (previous == null || previous.submitTime < output.submitTime) {
                map.put(render, output);
                if (previous != null) {
                    previous.deleteFully();
                }
            }
        }

        return new ArrayList<>(map.values());
    }

    private ArrayList<BuilderTaskOutput> collectChunkBuildResults() {
        ArrayList<BuilderTaskOutput> results = new ArrayList<>();
        ChunkJobResult<? extends BuilderTaskOutput> result;

        while ((result = this.buildResults.poll()) != null) {
            results.add(result.unwrap());
        }

        return results;
    }

    public void cleanupAndFlip() {
        this.sectionCache.cleanup();
        this.regions.update();
    }

    public void updateChunks(boolean updateImmediately) {
        var thisFrameBlockingCollector = this.lastBlockingCollector;
        if (thisFrameBlockingCollector == null) {
            thisFrameBlockingCollector = new ChunkJobCollector(Integer.MAX_VALUE, this.buildResults::add);
        }

        if (updateImmediately) {
            // for a perfect frame where everything is finished use the last frame's blocking collector
            // and add all tasks to it so that they're waited on
            this.submitSectionTasks(thisFrameBlockingCollector, thisFrameBlockingCollector, thisFrameBlockingCollector);

            thisFrameBlockingCollector.awaitCompletion(this.builder);
            this.lastBlockingCollector = null;
        } else {
            var nextFrameBlockingCollector = new ChunkJobCollector(Integer.MAX_VALUE, this.buildResults::add);
            var deferredCollector = new ChunkJobCollector(this.builder.getSchedulingBudget(), this.buildResults::add);

            // if zero frame delay is allowed, submit important sorts with the current frame blocking collector.
            // otherwise submit with the collector that the next frame is blocking on.
            if (allowZeroFrameSortWait()) {
                this.submitSectionTasks(thisFrameBlockingCollector, nextFrameBlockingCollector, deferredCollector);
            } else {
                this.submitSectionTasks(nextFrameBlockingCollector, nextFrameBlockingCollector, deferredCollector);
            }

            // wait on this frame's blocking collector which contains the important tasks from this frame
            // and semi-important tasks from the last frame
            thisFrameBlockingCollector.awaitCompletion(this.builder);

            // store the semi-important collector to wait on it in the next frame
            this.lastBlockingCollector = nextFrameBlockingCollector;
        }
    }

    private void submitSectionTasks(
        ChunkJobCollector importantCollector,
        ChunkJobCollector semiImportantCollector,
        ChunkJobCollector deferredCollector) {
            this.submitSectionTasks(importantCollector, ChunkUpdateType.IMPORTANT_SORT);
            this.submitSectionTasks(semiImportantCollector, ChunkUpdateType.IMPORTANT_REBUILD);
            this.submitSectionTasks(deferredCollector, ChunkUpdateType.REBUILD);
            this.submitSectionTasks(deferredCollector, ChunkUpdateType.INITIAL_BUILD);
            this.submitSectionTasks(deferredCollector, ChunkUpdateType.SORT);
    }

    private void submitSectionTasks(ChunkJobCollector collector, ChunkUpdateType type) {
        var queue = this.taskLists.get(type);

        while (!queue.isEmpty() && collector.canOffer()) {
            RenderSection section = queue.remove();

            if (section.isDisposed()) {
                continue;
            }

            // stop if the section is in this list but doesn't have this update type
            var pendingUpdate = section.getPendingUpdate();
            if (pendingUpdate != null && pendingUpdate != type) {
                continue;
            }

            int frame = this.lastUpdatedFrame;
            ChunkBuilderTask<? extends BuilderTaskOutput> task;
            if (type == ChunkUpdateType.SORT || type == ChunkUpdateType.IMPORTANT_SORT) {
                task = this.createSortTask(section, frame);
            } else {
                task = this.createRebuildTask(section, frame);
            }

            if (task != null) {
                var job = this.builder.scheduleTask(task, type.isImportant(), collector::onJobFinished);
                collector.addSubmittedJob(job);

                section.setTaskCancellationToken(job);
            } else {
                // if the section is empty, doesn't exist or no sort task needs to be created
                // for non-dynamic data, submit this null-task to set the built flag on the
                // render section.
                // It's important to use a NoData instead of null translucency data here in
                // order for it to clear the old data from the translucency sorting system
                var result = ChunkJobResult.successfully(new ChunkBuildOutput(
                        section, frame, new NoData(section.getPosition()),
                        BuiltSectionInfo.EMPTY, Collections.emptyMap()));
                this.buildResults.add(result);

                section.setTaskCancellationToken(null);
            }

            section.setLastSubmittedFrame(frame);
            section.setPendingUpdate(null);
        }
    }

    public @Nullable ChunkBuilderMeshingTask createRebuildTask(RenderSection render, int frame) {
        ChunkRenderContext context = WorldSlice.prepare(this.world, render.getPosition(), this.sectionCache);

        if (context == null) {
            return null;
        }

        return new ChunkBuilderMeshingTask(render, frame, this.cameraPosition, context);
    }

    public ChunkBuilderSortingTask createSortTask(RenderSection render, int frame) {
        return ChunkBuilderSortingTask.createTask(render, frame, this.cameraPosition);
    }

    public void processGFNIMovement(CameraMovement movement) {
        this.ts.triggerSections(this::scheduleSort, movement);
    }

    public void markGraphDirty() {
        this.needsGraphUpdate = true;
    }

    public boolean needsUpdate() {
        return this.needsGraphUpdate;
    }

    public ChunkBuilder getBuilder() {
        return this.builder;
    }

    public void destroy() {
        this.builder.shutdown(); // stop all the workers, and cancel any tasks

        for (var result : this.collectChunkBuildResults()) {
            result.deleteFully(); // delete resources for any pending tasks (including those that were cancelled)
        }

        for (var section : sectionByPosition.values()) {
            section.delete();
        }

        this.sectionsWithGlobalEntities.clear();
        this.resetRenderLists();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.regions.delete(commandList);
            this.chunkRenderer.delete(commandList);
        }
    }

    public int getTotalSections() {
        return this.sectionByPosition.size();
    }

    public int getVisibleChunkCount() {
        var sections = 0;
        var iterator = this.renderLists.iterator();

        while (iterator.hasNext()) {
            var renderList = iterator.next();
            sections += renderList.getSectionsWithGeometryCount();
        }

        return sections;
    }

    public void scheduleSort(long sectionPos, boolean isDirectTrigger) {
        RenderSection section = this.sectionByPosition.get(sectionPos);

        if (section != null) {
            var pendingUpdate = ChunkUpdateType.SORT;
            if (allowImportantSorts() && this.shouldPrioritizeTask(section)) {
                pendingUpdate = ChunkUpdateType.IMPORTANT_SORT;
            }
            pendingUpdate = ChunkUpdateType.getPromotionUpdateType(section.getPendingUpdate(), pendingUpdate);
            if (pendingUpdate != null) {
                section.setPendingUpdate(pendingUpdate);
                section.prepareTrigger(isDirectTrigger);
            }
        }
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        RenderAsserts.validateCurrentThread();

        this.sectionCache.invalidate(x, y, z);

        RenderSection section = this.sectionByPosition.get(ChunkSectionPos.asLong(x, y, z));

        if (section != null && section.isBuilt()) {
            ChunkUpdateType pendingUpdate;

            if (allowImportantRebuilds() && (important || this.shouldPrioritizeTask(section))) {
                pendingUpdate = ChunkUpdateType.IMPORTANT_REBUILD;
            } else {
                pendingUpdate = ChunkUpdateType.REBUILD;
            }

            pendingUpdate = ChunkUpdateType.getPromotionUpdateType(section.getPendingUpdate(), pendingUpdate);
            if (pendingUpdate != null) {
                section.setPendingUpdate(pendingUpdate);

                this.needsGraphUpdate = true;
            }
        }
    }

    private static final float NEARBY_REBUILD_DISTANCE = MathHelper.square(16.0f);

    private boolean shouldPrioritizeTask(RenderSection section) {
        return this.cameraPosition != null && section.getSquaredDistance(this.cameraBlockPos) < NEARBY_REBUILD_DISTANCE;
    }

    private static boolean allowImportantRebuilds() {
        return !SodiumClientMod.options().performance.alwaysDeferChunkUpdates;
    }

    private static boolean allowImportantSorts() {
        return SodiumClientMod.options().performance.deferSortMode != DeferSortMode.ALWAYS;
    }

    private static boolean allowZeroFrameSortWait() {
        return SodiumClientMod.options().performance.deferSortMode == DeferSortMode.DEFER_ZERO_FRAMES;
    }

    private float getEffectiveRenderDistance() {
        var color = RenderSystem.getShaderFogColor();
        var distance = RenderSystem.getShaderFogEnd();

        var renderDistance = this.getRenderDistance();

        // The fog must be fully opaque in order to skip rendering of chunks behind it
        if (!MathHelper.approximatelyEquals(color[3], 1.0f)) {
            return renderDistance;
        }

        return Math.min(renderDistance, distance + 0.5f);
    }

    private float getRenderDistance() {
        return this.renderDistance * 16.0f;
    }

    private void connectNeighborNodes(RenderSection render) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            RenderSection adj = this.getRenderSection(render.getChunkX() + GraphDirection.x(direction),
                    render.getChunkY() + GraphDirection.y(direction),
                    render.getChunkZ() + GraphDirection.z(direction));

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(direction), render);
                render.setAdjacentNode(direction, adj);
            }
        }
    }

    private void disconnectNeighborNodes(RenderSection render) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            RenderSection adj = render.getAdjacent(direction);

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(direction), null);
                render.setAdjacentNode(direction, null);
            }
        }
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sectionByPosition.get(ChunkSectionPos.asLong(x, y, z));
    }

    public Collection<String> getDebugStrings() {
        List<String> list = new ArrayList<>();

        int count = 0;

        long deviceUsed = 0;
        long deviceAllocated = 0;

        for (var region : this.regions.getLoadedRegions()) {
            var resources = region.getResources();

            if (resources == null) {
                continue;
            }

            var buffer = resources.getGeometryArena();

            deviceUsed += buffer.getDeviceUsedMemory();
            deviceAllocated += buffer.getDeviceAllocatedMemory();

            count++;
        }

        list.add(String.format("Geometry Pool: %d/%d MiB (%d buffers)", MathUtil.toMib(deviceUsed), MathUtil.toMib(deviceAllocated), count));
        list.add(String.format("Transfer Queue: %s", this.regions.getStagingBuffer().toString()));

        list.add(String.format("Chunk Builder: Permits=%02d | Busy=%02d | Total=%02d",
                this.builder.getScheduledJobCount(), this.builder.getBusyThreadCount(), this.builder.getTotalThreadCount())
        );

        list.add(String.format("Chunk Queues: U=%02d (P0=%03d | P1=%03d | P2=%03d)",
                this.buildResults.size(),
                this.taskLists.get(ChunkUpdateType.IMPORTANT_REBUILD).size(),
                this.taskLists.get(ChunkUpdateType.REBUILD).size(),
                this.taskLists.get(ChunkUpdateType.INITIAL_BUILD).size())
        );

        this.ts.addDebugStrings(list);

        return list;
    }

    public @NotNull SortedRenderLists getRenderLists() {
        return this.renderLists;
    }

    public boolean isSectionBuilt(int x, int y, int z) {
        var section = this.getRenderSection(x, y, z);
        return section != null && section.isBuilt();
    }

    public void onChunkAdded(int x, int z) {
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.onSectionAdded(x, y, z);
        }
    }

    public void onChunkRemoved(int x, int z) {
        for (int y = this.world.getBottomSectionCoord(); y < this.world.getTopSectionCoord(); y++) {
            this.onSectionRemoved(x, y, z);
        }
    }

    public Collection<RenderSection> getSectionsWithGlobalEntities() {
        return ReferenceSets.unmodifiable(this.sectionsWithGlobalEntities);
    }
}
