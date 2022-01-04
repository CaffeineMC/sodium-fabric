package me.jellysquid.mods.sodium.render.chunk;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.render.chunk.compile.tasks.TerrainBuildResult;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkGraphState;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkRenderBounds;
import me.jellysquid.mods.sodium.render.chunk.state.ChunkRenderData;
import me.jellysquid.mods.sodium.render.chunk.passes.ChunkRenderPass;
import me.jellysquid.mods.sodium.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.render.chunk.state.UploadedChunkMesh;
import me.jellysquid.mods.sodium.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.util.DirectionUtil;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import org.apache.commons.lang3.Validate;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The render state object for a chunk section. This contains all the graphics state for each render pass along with
 * data about the render in the chunk visibility graph.
 */
public class RenderSection {
    private final SodiumWorldRenderer worldRenderer;
    private final int chunkX, chunkY, chunkZ;

    private final Map<ChunkRenderPass, UploadedChunkMesh> meshes;
    private final RenderRegion region;
    private final ChunkGraphState graphState;
    private final int chunkId;

    private final RenderSection[] adjacent = new RenderSection[DirectionUtil.ALL_DIRECTIONS.length];

    private ChunkRenderData data = ChunkRenderData.ABSENT;
    private CompletableFuture<?> rebuildTask = null;

    private ChunkUpdateType pendingUpdate;

    private boolean tickable;
    private boolean disposed;

    private int lastAcceptedBuildTime = -1;

    public RenderSection(SodiumWorldRenderer worldRenderer, int chunkX, int chunkY, int chunkZ, RenderRegion region) {
        this.worldRenderer = worldRenderer;
        this.region = region;

        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        this.graphState = new ChunkGraphState(this);

        this.meshes = new Reference2ReferenceOpenHashMap<>();

        int rX = this.getChunkX() & (RenderRegion.REGION_WIDTH - 1);
        int rY = this.getChunkY() & (RenderRegion.REGION_HEIGHT - 1);
        int rZ = this.getChunkZ() & (RenderRegion.REGION_LENGTH - 1);

        this.chunkId = RenderRegion.getChunkIndex(rX, rY, rZ);
    }

    public RenderSection getAdjacent(Direction dir) {
        return this.adjacent[dir.ordinal()];
    }

    public void setAdjacentNode(Direction dir, RenderSection node) {
        this.adjacent[dir.ordinal()] = node;
    }

    /**
     * Cancels any pending tasks to rebuild the chunk. If the result of any pending tasks has not been processed yet,
     * those will also be discarded when processing finally happens.
     */
    public void cancelRebuildTask() {
        if (this.rebuildTask != null) {
            this.rebuildTask.cancel(false);
            this.rebuildTask = null;
        }
    }

    public ChunkRenderData getData() {
        return this.data;
    }

    /**
     * Deletes all data attached to this render and drops any pending tasks. This should be used when the render falls
     * out of view or otherwise needs to be destroyed. After the render has been destroyed, the object can no longer
     * be used.
     */
    public void delete() {
        this.cancelRebuildTask();
        this.setData(ChunkRenderData.ABSENT);
        this.deleteGraphicsState();

        this.disposed = true;
    }

    private void deleteGraphicsState() {
        for (UploadedChunkMesh state : this.meshes.values()) {
            state.delete();
        }

        this.meshes.clear();
    }

    public void setData(ChunkRenderData info) {
        if (info == null) {
            throw new NullPointerException("Mesh information must not be null");
        }

        this.worldRenderer.onChunkRenderUpdated(this.chunkX, this.chunkY, this.chunkZ, this.data, info);
        this.data = info;

        this.tickable = !info.getAnimatedSprites().isEmpty();
    }

    /**
     * @return True if the chunk render contains no data, otherwise false
     */
    public boolean isEmpty() {
        return this.meshes.isEmpty() && this.data.isEmpty();
    }

    /**
     * Returns the chunk section position which this render refers to in the world.
     */
    public ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
    }

    /**
     * Ensures that all resources attached to the given chunk render are "ticked" forward. This should be called every
     * time before this render is drawn if {@link RenderSection#isTickable()} is true.
     */
    public void tick() {
        for (Sprite sprite : this.data.getAnimatedSprites()) {
            SpriteUtil.markSpriteActive(sprite);
        }
    }

    /**
     * @return The x-coordinate of the origin position of this chunk render
     */
    public int getOriginX() {
        return this.chunkX << 4;
    }

    /**
     * @return The y-coordinate of the origin position of this chunk render
     */
    public int getOriginY() {
        return this.chunkY << 4;
    }

    /**
     * @return The z-coordinate of the origin position of this chunk render
     */
    public int getOriginZ() {
        return this.chunkZ << 4;
    }

    /**
     * @return The squared distance from the center of this chunk in the world to the center of the block position
     * given by {@param pos}
     */
    public double getSquaredDistance(BlockPos pos) {
        return this.getSquaredDistance(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    /**
     * @return The squared distance from the center of this chunk in the world to the given position
     */
    public double getSquaredDistance(double x, double y, double z) {
        double xDist = x - this.getCenterX();
        double yDist = y - this.getCenterY();
        double zDist = z - this.getCenterZ();

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }

    /**
     * @return The x-coordinate of the center position of this chunk render
     */
    private double getCenterX() {
        return this.getOriginX() + 8.0D;
    }

    /**
     * @return The y-coordinate of the center position of this chunk render
     */
    private double getCenterY() {
        return this.getOriginY() + 8.0D;
    }

    /**
     * @return The z-coordinate of the center position of this chunk render
     */
    private double getCenterZ() {
        return this.getOriginZ() + 8.0D;
    }

    public void addMesh(ChunkRenderPass pass, UploadedChunkMesh mesh) {
        Validate.isTrue(this.meshes.putIfAbsent(pass, mesh) == null);
    }

    public UploadedChunkMesh getMesh(ChunkRenderPass pass) {
        return this.meshes.get(pass);
    }

    /**
     * @return The squared distance from the center of this chunk in the world to the given position
     */
    public double getSquaredDistanceXZ(double x, double z) {
        double xDist = x - this.getCenterX();
        double zDist = z - this.getCenterZ();

        return (xDist * xDist) + (zDist * zDist);
    }

    public int getChunkX() {
        return this.chunkX;
    }

    public int getChunkY() {
        return this.chunkY;
    }

    public int getChunkZ() {
        return this.chunkZ;
    }

    public ChunkRenderBounds getBounds() {
        return this.data.getBounds();
    }

    public boolean isTickable() {
        return this.tickable;
    }

    public RenderRegion getRegion() {
        return this.region;
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    @Override
    public String toString() {
        return String.format("RenderChunk{chunkX=%d, chunkY=%d, chunkZ=%d}",
                this.chunkX, this.chunkY, this.chunkZ);
    }

    public ChunkGraphState getGraphInfo() {
        return this.graphState;
    }

    public void setOcclusionData(ChunkOcclusionData occlusionData) {
        this.graphState.setOcclusionData(occlusionData);
    }

    public ChunkUpdateType getPendingUpdate() {
        return this.pendingUpdate;
    }

    public void markForUpdate(ChunkUpdateType type) {
        if (this.pendingUpdate == null || type.ordinal() > this.pendingUpdate.ordinal()) {
            this.pendingUpdate = type;
        }
    }

    public void onBuildSubmitted(CompletableFuture<?> task) {
        if (this.rebuildTask != null) {
            this.rebuildTask.cancel(false);
            this.rebuildTask = null;
        }

        this.rebuildTask = task;
        this.pendingUpdate = null;
    }

    public boolean isBuilt() {
        return this.data != ChunkRenderData.ABSENT;
    }

    public boolean canAcceptBuildResults(TerrainBuildResult result) {
        return !this.isDisposed() && result.buildTime > this.lastAcceptedBuildTime;
    }

    public void onBuildFinished(TerrainBuildResult result) {
        this.setData(result.data);
        this.lastAcceptedBuildTime = result.buildTime;
    }

    public int getChunkId() {
        return this.chunkId;
    }

    public void deleteMeshes() {
        for (UploadedChunkMesh mesh : this.meshes.values()) {
            mesh.delete();
        }

        this.meshes.clear();
    }
}
