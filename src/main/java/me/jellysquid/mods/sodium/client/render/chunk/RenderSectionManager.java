package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.graph.Graph;
import me.jellysquid.mods.sodium.client.render.chunk.graph.Rasterizer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderEmptyBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import me.jellysquid.mods.sodium.common.util.collections.WorkStealingFutureDrain;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RenderSectionManager {
    private final ChunkBuilder builder;

    private final RenderRegionManager regions;
    private final EnumMap<ChunkUpdateType, PriorityQueue<RenderSection>> rebuildQueues = new EnumMap<>(ChunkUpdateType.class);
    private final ObjectList<RenderSection> tickableChunks = new ObjectArrayList<>();
    private final ObjectList<RenderSection> entityChunks = new ObjectArrayList<>();

    private final ChunkRenderList chunkRenderList;


    private final RegionChunkRenderer chunkRenderer;

    private final SodiumWorldRenderer worldRenderer;
    private final ClientWorld world;

    private float cameraX, cameraY, cameraZ;

    private boolean needsUpdate;

    private int currentFrame = 0;
    private boolean alwaysDeferChunkUpdates;
    private boolean useBlockFaceCulling;

    private final Long2ObjectOpenHashMap<RenderSection> sections = new Long2ObjectOpenHashMap<>();
    private final Graph graph;

    private final ChunkTracker chunkTracker;
    private final int renderDistance;

    private ChunkTracker.Area watchedArea;

    public RenderSectionManager(SodiumWorldRenderer worldRenderer, BlockRenderPassManager renderPassManager, ClientWorld world, int renderDistance, ChunkTracker chunkTracker, CommandList commandList) {
        this.chunkRenderer = new RegionChunkRenderer(RenderDevice.INSTANCE, ChunkModelVertexFormats.DEFAULT);
        this.graph = new Graph(world, renderDistance);

        this.worldRenderer = worldRenderer;
        this.world = world;

        this.builder = new ChunkBuilder(ChunkModelVertexFormats.DEFAULT);
        this.builder.init(world, renderPassManager);

        this.needsUpdate = true;

        this.regions = new RenderRegionManager(commandList);

        for (ChunkUpdateType type : ChunkUpdateType.values()) {
            this.rebuildQueues.put(type, new ObjectArrayFIFOQueue<>());
        }

        this.renderDistance = renderDistance;

        this.chunkRenderList = new ChunkRenderList(this.regions);
        this.chunkTracker = chunkTracker;
    }

    public void update(Camera camera, Frustum frustum, int frame, boolean spectator) {
        this.updateWatchedArea(camera);
        this.processChunkQueues();

        this.currentFrame = frame;

        var useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled;

        if (spectator && this.world.getBlockState(camera.getBlockPos())
                .isOpaqueFullCube(this.world, camera.getBlockPos())) {
            useOcclusionCulling = false;
        }

        this.resetLists();

        Vec3d cameraPos = camera.getPos();

        this.cameraX = (float) cameraPos.x;
        this.cameraY = (float) cameraPos.y;
        this.cameraZ = (float) cameraPos.z;

        this.alwaysDeferChunkUpdates = SodiumClientMod.options().performance.alwaysDeferChunkUpdates;
        this.useBlockFaceCulling = SodiumClientMod.options().performance.useBlockFaceCulling;

        this.rasterizer.clear();
        this.rasterizer.setCamera(camera, frustum);

        var search = this.graph.createSearch(camera, frustum, useOcclusionCulling);
        var list = search.start();

        var dist = 0;

        while (!list.isEmpty()) {
            var filteredPos = list.elements();
            var filteredCount = 0;

            for (int index = 0, count = list.size(); index < count; index++) {
                var sectionId = list.getLong(index);
                var section = this.sections.get(sectionId);

                if (!this.isVisibleOnRaster(section, dist)) {
                    continue;
                }

                this.addSectionToLists(section);
                this.drawOcclusionHull(section, dist);

                filteredPos[filteredCount++] = sectionId;
            }

            list = search.next(LongArrayList.wrap(filteredPos, filteredCount));
            dist++;
        }

        if (GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_P) == GLFW.GLFW_PRESS) {
            this.rasterizer.saveDepthBuffer("/dev/shm/fb.png");
        }

        this.needsUpdate = false;
    }

    private void drawOcclusionHull(RenderSection section, int dist) {
        var boxes = section.getData().getOcclusionBoxes();

        if (boxes != null && dist > 1) {
            this.rasterizer.drawBoxes(boxes[this.getOccluderDetailLevel(dist)], section.getOriginX(), section.getOriginY(), section.getOriginZ());
        }
    }

    private boolean isVisibleOnRaster(RenderSection section, int dist) {
        if (dist > 1) {
            return this.rasterizer.testBox(section.getOriginX(), section.getOriginY(), section.getOriginZ(),
                    section.getOriginX() + 16.0f, section.getOriginY() + 16.0f, section.getOriginZ() + 16.0f, 0b111111);
        }

        return true;
    }

    private void updateWatchedArea(Camera camera) {
        int x = camera.getBlockPos().getX() >> 4;
        int z = camera.getBlockPos().getZ() >> 4;

        var prevArea = this.watchedArea;
        var newArea = new ChunkTracker.Area(x, z, this.renderDistance);

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

    protected final Rasterizer rasterizer = new Rasterizer(864, 480);

    private void addSectionToLists(RenderSection section) {
        if (section.getPendingUpdate() != null) {
            var queue = this.rebuildQueues.get(section.getPendingUpdate());

            if (queue.size() < 32) {
                queue.enqueue(section);
            }
        }

        if (section.hasFlag(ChunkDataFlags.HAS_BLOCK_GEOMETRY)) {
            this.chunkRenderList.add(section, this.getVisibleFaces(section));
        }

        if (section.hasFlag(ChunkDataFlags.HAS_ANIMATED_SPRITES)) {
            this.tickableChunks.add(section);
        }

        if (section.hasFlag(ChunkDataFlags.HAS_BLOCK_ENTITIES)) {
            this.entityChunks.add(section);
        }
    }

    private int getOccluderDetailLevel(int dist) {
        if (dist < 2) {
            return 0;
        } else if (dist < 12) {
            return 1;
        } else {
            return 2;
        }
    }

    private int getVisibleFaces(RenderSection section) {
        if (this.useBlockFaceCulling) {
            var bounds = section.getBounds();

            int faces = ModelQuadFacing.BIT_UNASSIGNED;

            if (this.cameraY > bounds.y1) {
                faces |= ModelQuadFacing.BIT_UP;
            }

            if (this.cameraY < bounds.y2) {
                faces |= ModelQuadFacing.BIT_DOWN;
            }

            if (this.cameraX > bounds.x1) {
                faces |= ModelQuadFacing.BIT_EAST;
            }

            if (this.cameraX < bounds.x2) {
                faces |= ModelQuadFacing.BIT_WEST;
            }

            if (this.cameraZ > bounds.z1) {
                faces |= ModelQuadFacing.BIT_SOUTH;
            }

            if (this.cameraZ < bounds.z2) {
                faces |= ModelQuadFacing.BIT_NORTH;
            }

            return faces;
        } else {
            return ModelQuadFacing.BIT_ALL;
        }
    }

    private void resetLists() {
        for (var queue : this.rebuildQueues.values()) {
            queue.clear();
        }

        this.entityChunks.clear();
        this.chunkRenderList.clear();
        this.tickableChunks.clear();
    }

    public Iterator<BlockEntity> getVisibleBlockEntities() {
        return this.entityChunks.stream()
                .flatMap(section -> section.getData()
                        .getBlockEntities()
                        .stream())
                .iterator();
    }

    public void renderLayer(ChunkRenderMatrices matrices, BlockRenderPass pass, double x, double y, double z) {
        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrices, commandList, this.chunkRenderList, pass, new ChunkCameraContext(x, y, z));

        commandList.flush();
    }

    public void tickVisibleRenders() {
        for (RenderSection section : this.tickableChunks) {
            section.tick();
        }
    }

    public boolean isSectionVisible(int x, int y, int z) {
        return this.graph.isSectionVisible(x, y, z);
    }

    public void updateChunks() {
        this.updateChunks(false);
    }

    public void updateAllChunksNow() {
        this.updateChunks(true);

        // Also wait for any rebuilds which had already been scheduled before this method was called
        this.needsUpdate |= this.performAllUploads();
    }

    private void updateChunks(boolean allImmediately) {
        var blockingFutures = new LinkedList<CompletableFuture<ChunkBuildResult>>();

        var sectionCache = new ClonedChunkSectionCache(this.world);

        this.submitRebuildTasks(ChunkUpdateType.IMPORTANT_REBUILD, blockingFutures, sectionCache);
        this.submitRebuildTasks(ChunkUpdateType.INITIAL_BUILD, allImmediately ? blockingFutures : null, sectionCache);
        this.submitRebuildTasks(ChunkUpdateType.REBUILD, allImmediately ? blockingFutures : null, sectionCache);

        // Try to complete some other work on the main thread while we wait for rebuilds to complete
        this.needsUpdate |= this.processBuiltChunks(this.builder.createAsyncResultDrain());

        if (!blockingFutures.isEmpty()) {
            this.needsUpdate = this.processBuiltChunks(new WorkStealingFutureDrain<>(blockingFutures, this.builder::stealTask));
        }

        this.regions.cleanup();
    }

    private boolean processBuiltChunks(Iterator<ChunkBuildResult> it) {
        var results = collectBuiltChunks(it);

        this.regions.uploadMeshes(RenderDevice.INSTANCE.createCommandList(), results);

        for (var result : results) {
            this.updateSectionData(result);
            result.delete();
        }

        return !results.isEmpty();
    }

    private static ArrayList<ChunkBuildResult> collectBuiltChunks(Iterator<ChunkBuildResult> it) {
        var results = new ArrayList<ChunkBuildResult>();

        while (it.hasNext()) {
            var result = it.next();
            var section = result.section;

            if (section.isDisposed() || result.timestamp < section.getLastRebuildTime()) {
                result.delete();
                continue;
            }

            results.add(result);
        }

        return results;
    }

    private void updateSectionData(ChunkBuildResult result) {
        var section = result.section;

        this.worldRenderer.onChunkRenderUpdated(section.getChunkX(), section.getChunkY(), section.getChunkZ(),
                section.getData(), result.data);

        section.setData(result.data);
        section.finishRebuild();
    }

    private void submitRebuildTasks(ChunkUpdateType updateType, LinkedList<CompletableFuture<ChunkBuildResult>> immediateFutures, ClonedChunkSectionCache sectionCache) {
        int budget = immediateFutures != null ? Integer.MAX_VALUE : this.builder.getSchedulingBudget();

        PriorityQueue<RenderSection> queue = this.rebuildQueues.get(updateType);

        var frame = this.currentFrame;

        while (budget > 0 && !queue.isEmpty()) {
            RenderSection section = queue.dequeue();

            if (section.isDisposed()) {
                continue;
            }

            // Sections can move between update queues, but they won't be removed from the queue they were
            // previously in to save CPU cycles. We just filter any changed entries here instead.
            if (section.getPendingUpdate() != updateType) {
                continue;
            }

            section.cancelRebuild();

            ChunkRenderBuildTask task = this.createRebuildTask(sectionCache, section, frame);
            CompletableFuture<?> future;

            if (immediateFutures != null) {
                CompletableFuture<ChunkBuildResult> immediateFuture = this.builder.schedule(task);
                immediateFutures.add(immediateFuture);

                future = immediateFuture;
            } else {
                future = this.builder.scheduleDeferred(task);
            }

            section.setRebuildFuture(future, frame);

            budget--;
        }
    }

    /**
     * Processes all build task uploads, blocking for tasks to complete if necessary.
     */
    private boolean performAllUploads() {
        boolean anythingUploaded = false;

        while (true) {
            // First check if all tasks are done building (and therefore the upload queue is final)
            boolean allTasksBuilt = this.builder.isIdle();

            // Then process the entire upload queue
            anythingUploaded |= this.processBuiltChunks(this.builder.createAsyncResultDrain());

            // If the upload queue was the final one
            if (allTasksBuilt) {
                // then we are done
                return anythingUploaded;
            } else {
                // otherwise we need to wait for the worker threads to make progress
                try {
                    // This code path is not the default one, it doesn't need super high performance, and having the
                    // workers notify the main thread just for it is probably not worth it.
                    //noinspection BusyWait
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return true;
                }
            }
        }
    }

    public ChunkRenderBuildTask createRebuildTask(ClonedChunkSectionCache sectionCache, RenderSection render, int frame) {
        ChunkRenderContext context = WorldSlice.prepare(this.world, render.getChunkPos(), sectionCache);

        if (context == null) {
            return new ChunkRenderEmptyBuildTask(render, frame);
        }

        return new ChunkRenderRebuildTask(render, context, frame);
    }

    public void markGraphDirty() {
        this.needsUpdate = true;
    }

    public boolean isGraphDirty() {
        return this.needsUpdate | true;
    }

    public ChunkBuilder getBuilder() {
        return this.builder;
    }

    public void destroy() {
        this.resetLists();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.regions.delete(commandList);
        }

        this.chunkRenderer.delete();
        this.builder.stopWorkers();

        if (this.watchedArea != null) {
            this.chunkTracker.removeWatchedArea(this.watchedArea);
        }
    }

    public int getTotalSections() {
        return this.sections.size();
    }

    public int getVisibleChunkCount() {
        return this.chunkRenderList.getCount();
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        RenderSection section = this.getSection(x, y, z);

        if (section != null && section.isBuilt()) {
            if (!this.alwaysDeferChunkUpdates && important) {
                section.markForUpdate(ChunkUpdateType.IMPORTANT_REBUILD);
            } else {
                section.markForUpdate(ChunkUpdateType.REBUILD);
            }

            this.needsUpdate = true;
        }
    }

    private RenderSection getSection(int x, int y, int z) {
        return this.sections.get(ChunkSectionPos.asLong(x, y, z));
    }

    public void onChunkRenderUpdates(int x, int y, int z, ChunkRenderData data) {
        this.graph.updateConnections(x, y, z, data.getOcclusionData());
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

        List<String> list = new ArrayList<>();
        list.add(String.format("Chunk arena allocator: %s", SodiumClientMod.options().advanced.arenaMemoryAllocator.name()));
        list.add(String.format("Device buffer objects: %d", count));
        list.add(String.format("Device memory: %d/%d MiB", MathUtil.toMib(deviceUsed), MathUtil.toMib(deviceAllocated)));
        list.add(String.format("Staging buffer: %s", this.regions.getStagingBuffer().toString()));

        return list;
    }

    private void loadSection(int x, int y, int z) {
        long pos = ChunkSectionPos.asLong(x, y, z);

        if (this.sections.containsKey(pos)) {
            throw new IllegalStateException("Section is already loaded [x=%s, y=%s, z=%s]".formatted(x, y, z));
        }

        RenderSection render = new RenderSection(x, y, z);

        Chunk chunk = this.world.getChunk(x, z);
        ChunkSection section = chunk.getSectionArray()[this.world.sectionCoordToIndex(y)];

        if (section.isEmpty()) {
            render.setData(ChunkRenderData.EMPTY);
        } else {
            render.markForUpdate(ChunkUpdateType.INITIAL_BUILD);
        }

        this.sections.put(pos, render);

        this.graph.loadSection(x, y, z);
    }

    private void unloadSection(int x, int y, int z) {
        var pos = ChunkSectionPos.asLong(x, y, z);

        if (!this.sections.containsKey(pos)) {
            throw new IllegalStateException("Section is not loaded [x=%s, y=%s, z=%s]".formatted(x, y, z));
        }

        var section = this.sections.remove(pos);
        section.cancelRebuild();
        section.dispose();

        var region = this.regions.getRegion(section.getRegionId());

        if (region != null) {
            region.deleteChunk(section.getLocalId());
        }

        this.graph.removeSection(x, y, z);
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

    public boolean isSectionReady(int x, int y, int z) {
        var section = this.getSection(x, y, z);

        if (section != null) {
            return section.getData() != ChunkRenderData.ABSENT;
        }

        return false;
    }
}
