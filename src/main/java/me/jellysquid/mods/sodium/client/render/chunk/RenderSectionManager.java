package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.BuilderTaskOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkJobResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderSortingTask;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.gfni.GFNI;
import me.jellysquid.mods.sodium.client.render.chunk.graph.GraphDirection;
import me.jellysquid.mods.sodium.client.render.chunk.graph.VisibilityEncoding;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderListBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.util.BitwiseMath;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.sorting.MergeSort;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;
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
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RenderSectionManager {
    private final ChunkBuilder builder;

    private final RenderRegionManager regions;
    private final ClonedChunkSectionCache sectionCache;

    private final Long2ReferenceMap<RenderSection> sectionByPosition = new Long2ReferenceOpenHashMap<>();

    private final EnumMap<ChunkUpdateType, ArrayDeque<RenderSection>> rebuildQueues = new EnumMap<>(ChunkUpdateType.class);

    private final ConcurrentLinkedDeque<ChunkJobResult<? extends BuilderTaskOutput>> buildResults = new ConcurrentLinkedDeque<>();

    private final ArrayDeque<RenderSection> iterationQueue = new ArrayDeque<>();

    private final ChunkRenderer chunkRenderer;

    private final ClientWorld world;
    private final GFNI gfni;

    private final int renderDistance;
    private int effectiveRenderDistance;

    private Vector3f cameraPos = new Vector3f();
    private int centerChunkX, centerChunkY, centerChunkZ;

    private boolean needsUpdate;

    private boolean useOcclusionCulling;
    private boolean useBlockFaceCulling;

    private int currentFrame = 0;
    private boolean alwaysDeferChunkUpdates;

    @NotNull
    private final SortedRenderListBuilder renderListBuilder;
    @NotNull
    private SortedRenderLists renderLists;

    private final ReferenceSet<RenderSection> sectionsWithGlobalEntities = new ReferenceOpenHashSet<>();

    public RenderSectionManager(ClientWorld world, int renderDistance, CommandList commandList) {
        this.chunkRenderer = new RegionChunkRenderer(RenderDevice.INSTANCE, ChunkMeshFormats.COMPACT);

        this.world = world;
        this.gfni = new GFNI();

        this.builder = new ChunkBuilder(world, ChunkMeshFormats.COMPACT);

        this.needsUpdate = true;
        this.renderDistance = renderDistance;

        this.regions = new RenderRegionManager(commandList);
        this.sectionCache = new ClonedChunkSectionCache(this.world);

        for (ChunkUpdateType type : ChunkUpdateType.values()) {
            this.rebuildQueues.put(type, new ArrayDeque<>());
        }

        this.renderListBuilder = new SortedRenderListBuilder();
        this.renderLists = SortedRenderLists.empty();
    }

    public void updateRenderLists(Camera camera, Viewport viewport, int frame, boolean spectator) {
        this.resetRenderLists();

        var renderListBuilder = this.renderListBuilder;
        renderListBuilder.reset();

        var options = SodiumClientMod.options();
        this.alwaysDeferChunkUpdates = options.performance.alwaysDeferChunkUpdates;

        this.searchChunks(renderListBuilder, camera, viewport, frame, spectator);

        this.renderLists = renderListBuilder.build();
        this.needsUpdate = false;
    }

    private void searchChunks(SortedRenderListBuilder renderListBuilder, Camera camera, Viewport viewport, int frame, boolean spectator) {
        this.initSearch(camera, viewport, frame, spectator);

        while (!this.iterationQueue.isEmpty()) {
            RenderSection section = this.iterationQueue.remove();

            int distance = this.getDistanceFromCamera(section);

            if (distance > this.effectiveRenderDistance || (distance > 0 && this.isOutsideViewport(section, viewport))) {
                continue;
            }

            this.addToRenderLists(renderListBuilder, section);

            if (section.getPendingUpdate() != null && section.getBuildCancellationToken() == null) {
                this.addToRebuildLists(section);
            }

            int connections;

            if (this.useOcclusionCulling) {
                connections = VisibilityEncoding.getConnections(section.getVisibilityData(), section.getIncomingDirections());
            } else {
                connections = GraphDirection.ALL;
            }

            connections &= this.getOutwardDirections(section.getChunkX(), section.getChunkY(), section.getChunkZ());

            if (connections != GraphDirection.NONE) {
                this.searchNeighbors(section, connections);
            }
        }
    }

    private void addToRenderLists(SortedRenderListBuilder renderListBuilder, RenderSection section) {
        renderListBuilder.add(section, this.getVisibleFaces(section.getChunkX(), section.getChunkY(), section.getChunkZ()));
    }

    private static final int MODEL_UNASSIGNED = ModelQuadFacing.UNASSIGNED.ordinal();
    private static final int MODEL_POS_X      = ModelQuadFacing.POS_X.ordinal();
    private static final int MODEL_NEG_X      = ModelQuadFacing.NEG_X.ordinal();
    private static final int MODEL_POS_Y      = ModelQuadFacing.POS_Y.ordinal();
    private static final int MODEL_NEG_Y      = ModelQuadFacing.NEG_Y.ordinal();
    private static final int MODEL_POS_Z      = ModelQuadFacing.POS_Z.ordinal();
    private static final int MODEL_NEG_Z      = ModelQuadFacing.NEG_Z.ordinal();

    private int getVisibleFaces(int x, int y, int z) {
        if (!this.useBlockFaceCulling) {
            return ModelQuadFacing.ALL;
        }

        // This is carefully written so that we can keep everything branch-less.
        //
        // Normally, this would be a ridiculous way to handle the problem. But the Hotspot VM's
        // heuristic for generating SETcc/CMOV instructions is broken, and it will always create a
        // branch even when a trivial ternary is encountered.
        //
        // For example, the following will never be transformed into a SETcc:
        //   (a > b) ? 1 : 0
        //
        // So we have to instead rely on sign-bit extension and masking (which generates a ton
        // of unnecessary instructions) to get this to be branch-less.
        //
        // To do this, we can transform the previous expression into the following.
        //   (b - a) >> 31
        //
        // This works because if (a > b) then (b - a) will always create a negative number. We then shift the sign bit
        // into the least significant bit's position (which also discards any bits following the sign bit) to get the
        // output we are looking for.
        //
        // If you look at the output which LLVM produces for a series of ternaries, you will instantly become distraught,
        // because it manages to a) correctly evaluate the cost of instructions, and b) go so far
        // as to actually produce vector code.  (https://godbolt.org/z/GaaEx39T9)

        int planes = 0;

        planes |= BitwiseMath.lessThan(x - 1, this.centerChunkX) << MODEL_POS_X;
        planes |= BitwiseMath.lessThan(y - 1, this.centerChunkY) << MODEL_POS_Y;
        planes |= BitwiseMath.lessThan(z - 1, this.centerChunkZ) << MODEL_POS_Z;

        planes |= BitwiseMath.greaterThan(x + 1, this.centerChunkX) << MODEL_NEG_X;
        planes |= BitwiseMath.greaterThan(y + 1, this.centerChunkY) << MODEL_NEG_Y;
        planes |= BitwiseMath.greaterThan(z + 1, this.centerChunkZ) << MODEL_NEG_Z;

        // the "unassigned" plane is always front-facing, since we can't check it
        planes |= (1 << MODEL_UNASSIGNED);

        return planes;
    }

    private void searchNeighbors(RenderSection section, int outgoing) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            if ((outgoing & (1 << direction)) == 0) {
                continue;
            }

            RenderSection adj = section.getAdjacent(direction);

            if (adj != null) {
                this.bfsEnqueue(adj, 1 << GraphDirection.opposite(direction));
            }
        }
    }

    private int getOutwardDirections(int x, int y, int z) {
        int planes = 0;

        planes |= x <= this.centerChunkX ? 1 << GraphDirection.WEST  : 0;
        planes |= x >= this.centerChunkX ? 1 << GraphDirection.EAST  : 0;

        planes |= y <= this.centerChunkY ? 1 << GraphDirection.DOWN  : 0;
        planes |= y >= this.centerChunkY ? 1 << GraphDirection.UP    : 0;

        planes |= z <= this.centerChunkZ ? 1 << GraphDirection.NORTH : 0;
        planes |= z >= this.centerChunkZ ? 1 << GraphDirection.SOUTH : 0;

        return planes;
    }

    private static final double CHUNK_RENDER_BOUNDS_EPSILON = 1.0D / 32.0D;

    private boolean isOutsideViewport(RenderSection section, Viewport viewport) {
        double x = section.getOriginX();
        double y = section.getOriginY();
        double z = section.getOriginZ();

        double minX = x - CHUNK_RENDER_BOUNDS_EPSILON;
        double minY = y - CHUNK_RENDER_BOUNDS_EPSILON;
        double minZ = z - CHUNK_RENDER_BOUNDS_EPSILON;

        double maxX = x + 16.0D + CHUNK_RENDER_BOUNDS_EPSILON;
        double maxY = y + 16.0D + CHUNK_RENDER_BOUNDS_EPSILON;
        double maxZ = z + 16.0D + CHUNK_RENDER_BOUNDS_EPSILON;

        return !viewport.isBoxVisible(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void addToRebuildLists(RenderSection section) {
        Queue<RenderSection> queue = this.rebuildQueues.get(section.getPendingUpdate());
        queue.add(section);
    }

    private void resetRenderLists() {
        for (Queue<RenderSection> queue : this.rebuildQueues.values()) {
            queue.clear();
        }

        this.renderLists = SortedRenderLists.empty();
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

        this.needsUpdate = true;
    }

    public void onSectionRemoved(int x, int y, int z) {
        long chunkSectionLongPos = ChunkSectionPos.asLong(x, y, z);
        RenderSection section = this.sectionByPosition.remove(chunkSectionLongPos);

        if (section == null) {
            return;
        }

        RenderRegion region = section.getRegion();

        if (region != null) {
            region.removeSection(section);
        }

        this.disconnectNeighborNodes(section);
        this.updateSectionInfo(section, null);

        section.delete();

        this.gfni.removeSection(chunkSectionLongPos);

        this.needsUpdate = true;
    }

    public void renderLayer(ChunkRenderMatrices matrices, TerrainRenderPass pass, double x, double y, double z) {
        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrices, commandList, this.regions, this.renderLists, pass, new ChunkCameraContext(x, y, z));

        commandList.flush();
    }

    public void tickVisibleRenders() {
        Iterator<ChunkRenderList> it = this.renderLists.sorted();

        while (it.hasNext()) {
            ChunkRenderList renderList = it.next();

            var region = renderList.getRegion();
            var iterator = renderList.sectionsWithSpritesIterator();

            if (iterator == null) {
                continue;
            }

            while (iterator.hasNext()) {
                var section = region.getSection(iterator.next());

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

        return render.getLastVisibleFrame() == this.currentFrame;
    }

    public void updateChunks(boolean updateImmediately) {
        this.sectionCache.cleanup();
        this.regions.update();

        this.submitRebuildTasks(ChunkUpdateType.TRANSLUCENT_SORT, true);
        this.submitRebuildTasks(ChunkUpdateType.IMPORTANT_REBUILD, false);
        this.submitRebuildTasks(ChunkUpdateType.REBUILD, !updateImmediately);
        this.submitRebuildTasks(ChunkUpdateType.INITIAL_BUILD, !updateImmediately);
    }

    public void uploadChunks() {
        this.waitForBlockingTasks();

        var results = this.collectChunkBuildResults();

        if (!results.isEmpty()) {
            this.processChunkBuildResults(results);

            for (var result : results) {
                result.delete();
            }

            this.needsUpdate = true;
        }
    }

    private void processChunkBuildResults(ArrayList<BuilderTaskOutput> results) {
        var filtered = filterChunkBuildResults(results);

        this.regions.uploadMeshes(RenderDevice.INSTANCE.createCommandList(), filtered);

        for (var result : filtered) {
            this.updateSectionInfo(result.render, result.info);

            var job = result.render.getBuildCancellationToken();

            if (job != null && result.buildTime >= result.render.getLastSubmittedFrame()) {
                result.render.setBuildCancellationToken(null);
            }

            result.render.setLastBuiltFrame(result.buildTime);
        }
    }

    private void updateSectionInfo(RenderSection render, BuiltSectionInfo info) {
        // TODO: make this work with translucent data and figure out a nice way to use BuilderTaskOutput
        render.setInfo(info);

        if (info == null || ArrayUtils.isEmpty(info.globalBlockEntities)) {
            this.sectionsWithGlobalEntities.remove(render);
        } else {
            this.sectionsWithGlobalEntities.add(render);
        }
    }

    private static List<ChunkBuildOutput> filterChunkBuildResults(ArrayList<BuilderTaskOutput> outputs) {
        // TODO: handle sort results in this method. the types should be split into two lists
        var map = new Reference2ReferenceLinkedOpenHashMap<RenderSection, ChunkBuildOutput>();

        for (var output : outputs) {
            if (!(output instanceof ChunkBuildOutput) && output.render.isDisposed() || output.render.getLastBuiltFrame() > output.buildTime) {
                continue;
            }

            var render = output.render;
            var previous = map.get(render);

            if (previous == null || previous.buildTime < output.buildTime) {
                map.put(render, (ChunkBuildOutput) output);
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

    private void waitForBlockingTasks() {
        boolean shouldContinue;

        do {
            shouldContinue = this.builder.stealBlockingTask();
        } while (shouldContinue);
    }

    private void submitRebuildTasks(ChunkUpdateType type, boolean asynchronous) {
        var budget = asynchronous ? this.builder.getSchedulingBudget() : Integer.MAX_VALUE;
        var queue = this.rebuildQueues.get(type);

        while (budget > 0 && !queue.isEmpty()) {
            RenderSection section = queue.poll();

            if (section.isDisposed()) {
                continue;
            }

            int frame = this.currentFrame;
            ChunkBuilderTask<? extends BuilderTaskOutput> task;
            if (type == ChunkUpdateType.TRANSLUCENT_SORT) {
                task = this.createSortTask(section);
            } else {
                task = this.createRebuildTask(section, frame);
            }

            if (task != null) {
                CancellationToken token = this.builder.scheduleTask(task, asynchronous, this.buildResults::add);
                section.setBuildCancellationToken(token);
            } else {
                // TODO: why does this exist and where is this data read? is null translucent data ok?
                var result = ChunkJobResult.successfully(new ChunkBuildOutput(section, frame, null, BuiltSectionInfo.EMPTY, Collections.emptyMap()));
                this.buildResults.add(result);

                section.setBuildCancellationToken(null);
            }

            section.setLastSubmittedFrame(frame);
            section.setPendingUpdate(null);

            budget--;
        }
    }

    public @Nullable ChunkBuilderMeshingTask createRebuildTask(RenderSection render, int frame) {
        ChunkRenderContext context = WorldSlice.prepare(this.world, render.getChunkPos(), this.sectionCache);

        if (context == null) {
            return null;
        }

        return new ChunkBuilderMeshingTask(render, frame, this.cameraPos, context, this.gfni);
    }

    public ChunkBuilderSortingTask createSortTask(RenderSection render) {
        if (render.getTranslucentData() == null) {
            return null;
        }

        return new ChunkBuilderSortingTask(render, this.currentFrame, this.cameraPos);
    }

    public void processGFNIMovement(
        double lastCameraX, double lastCameraY, double lastCameraZ,
        double cameraX, double cameraY, double cameraZ) {
        this.gfni.triggerSections(this::scheduleSort,
            lastCameraX, lastCameraY, lastCameraZ,
            cameraX, cameraY, cameraZ);
    }

    public void markGraphDirty() {
        this.needsUpdate = true;
    }

    public boolean needsUpdate() {
        return this.needsUpdate;
    }

    public ChunkBuilder getBuilder() {
        return this.builder;
    }

    public void destroy() {
        this.builder.shutdown(); // stop all the workers, and cancel any tasks

        for (var result : this.collectChunkBuildResults()) {
            result.delete(); // delete resources for any pending tasks (including those that were cancelled)
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
        var iterator = this.renderLists.sorted();

        while (iterator.hasNext()) {
            var renderList = iterator.next();
            sections += renderList.getSectionsWithGeometryCount();
        }

        return sections;
    }

    public void scheduleSort(ChunkSectionPos pos) {
        // TODO: Does this need to invalidate the section cache?

        RenderSection section = this.sectionByPosition.get(pos.asLong());

        if (section != null && ChunkUpdateType.canPromote(section.getPendingUpdate(), ChunkUpdateType.TRANSLUCENT_SORT)) {
            section.setPendingUpdate(ChunkUpdateType.TRANSLUCENT_SORT);
        }
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        this.sectionCache.invalidate(x, y, z);

        RenderSection section = this.sectionByPosition.get(ChunkSectionPos.asLong(x, y, z));

        if (section != null && section.isBuilt()) {
            ChunkUpdateType pendingUpdate;

            if (!this.alwaysDeferChunkUpdates && important) {
                pendingUpdate = ChunkUpdateType.IMPORTANT_REBUILD;
            } else {
                pendingUpdate = ChunkUpdateType.REBUILD;
            }

            if (ChunkUpdateType.canPromote(section.getPendingUpdate(), pendingUpdate)) {
                section.setPendingUpdate(pendingUpdate);
            }
        }

        this.needsUpdate = true;
    }

    private int getDistanceFromCamera(RenderSection section) {
        int x = Math.abs(section.getChunkX() - this.centerChunkX);
        int y = Math.abs(section.getChunkY() - this.centerChunkY);
        int z = Math.abs(section.getChunkZ() - this.centerChunkZ);

        return Math.max(x, Math.max(y, z));
    }

    private void initSearch(Camera camera, Viewport viewport, int frame, boolean spectator) {
        this.iterationQueue.clear();

        this.currentFrame = frame;

        var options = SodiumClientMod.options();

        if (options.performance.useFogOcclusion) {
            this.effectiveRenderDistance = Math.min(this.getEffectiveViewDistance(), this.renderDistance);
        } else {
            this.effectiveRenderDistance = this.renderDistance;
        }

        this.useBlockFaceCulling = options.performance.useBlockFaceCulling;

        BlockPos origin = camera.getBlockPos();

        if (spectator && this.world.getBlockState(origin).isOpaqueFullCube(this.world, origin)) {
            this.useOcclusionCulling = false;
        } else {
            this.useOcclusionCulling = MinecraftClient.getInstance().chunkCullingEnabled;
        }

        this.centerChunkX = origin.getX() >> 4;
        this.centerChunkY = origin.getY() >> 4;
        this.centerChunkZ = origin.getZ() >> 4;

        if (this.centerChunkY < this.world.getBottomSectionCoord()) {
            this.initSearchFallback(viewport, origin, this.centerChunkX, this.world.getBottomSectionCoord(), this.centerChunkZ, 1 << GraphDirection.DOWN);
        } else if (this.centerChunkY >= this.world.getTopSectionCoord()) {
            this.initSearchFallback(viewport, origin, this.centerChunkX, this.world.getTopSectionCoord() - 1, this.centerChunkZ, 1 << GraphDirection.UP);
        } else {
            var node = this.getRenderSection(this.centerChunkX, this.centerChunkY, this.centerChunkZ);

            if (node != null) {
                this.bfsEnqueue(node, GraphDirection.ALL);
            }
        }
    }

    private void initSearchFallback(Viewport viewport, BlockPos origin, int chunkX, int chunkY, int chunkZ, int directions) {
        List<RenderSection> sections = new ArrayList<>();

        for (int x2 = -this.effectiveRenderDistance; x2 <= this.effectiveRenderDistance; ++x2) {
            for (int z2 = -this.effectiveRenderDistance; z2 <= this.effectiveRenderDistance; ++z2) {
                RenderSection section = this.getRenderSection(chunkX + x2, chunkY, chunkZ + z2);

                if (section == null || this.isOutsideViewport(section, viewport)) {
                    continue;
                }

                sections.add(section);
            }
        }

        if (!sections.isEmpty()) {
            this.bfsEnqueueAll(sections, origin, directions);
        }
    }

    private void bfsEnqueueAll(List<RenderSection> sections, BlockPos origin, int directions) {
        final var distance = new float[sections.size()];

        for (int index = 0; index < sections.size(); index++) {
            var section = sections.get(index);
            distance[index] = -section.getSquaredDistance(origin); // sort by closest to camera
        }

        // TODO: avoid indirect sort via indices
        for (int index : MergeSort.mergeSort(distance)) {
            this.bfsEnqueue(sections.get(index), directions);
        }
    }

    private void bfsEnqueue(RenderSection render, int incomingDirections) {
        if (render.getLastVisibleFrame() != this.currentFrame) {
            render.setLastVisibleFrame(this.currentFrame);
            render.setIncomingDirections(GraphDirection.NONE);

            this.iterationQueue.add(render);
        }

        render.addIncomingDirections(incomingDirections);
    }

    private int getEffectiveViewDistance() {
        var color = RenderSystem.getShaderFogColor();
        var distance = RenderSystem.getShaderFogEnd();

        // The fog must be fully opaque in order to skip rendering of chunks behind it
        if (!MathHelper.approximatelyEquals(color[3], 1.0f)) {
            return this.renderDistance;
        }

        return MathHelper.ceil(distance / 16.0f);
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

        list.add(String.format("Device buffer objects: %d", count));
        list.add(String.format("Device memory: %d/%d MiB", MathUtil.toMib(deviceUsed), MathUtil.toMib(deviceAllocated)));
        list.add(String.format("Staging buffer: %s", this.regions.getStagingBuffer().toString()));

        list.add(String.format("Chunk builder: P=%02d | A=%02d | I=%02d",
                this.builder.getScheduledJobCount(), this.builder.getBusyThreadCount(), this.builder.getTotalThreadCount())
        );
        list.add(String.format("Chunk updates: U=%02d (P0=%03d | P1=%03d | P2=%05d)",
                this.buildResults.size(),
                this.rebuildQueues.get(ChunkUpdateType.IMPORTANT_REBUILD).size(),
                this.rebuildQueues.get(ChunkUpdateType.REBUILD).size(),
                this.rebuildQueues.get(ChunkUpdateType.INITIAL_BUILD).size())
        );

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
