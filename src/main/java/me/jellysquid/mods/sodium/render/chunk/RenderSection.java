package me.jellysquid.mods.sodium.render.chunk;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import me.jellysquid.mods.sodium.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.render.chunk.renderer.ChunkGraphicsState;
import me.jellysquid.mods.sodium.render.texture.SpriteUtil;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.joml.FrustumIntersection;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The render state object for a chunk section. This contains all the graphics state for each render pass along with
 * data about the render in the chunk visibility graph.
 */
public class RenderSection {
    private final RenderSectionManager manager;

    public final int id;
    
    private final int chunkX, chunkY, chunkZ;

    private final Map<BlockRenderPass, ChunkGraphicsState> graphicsStates;
    private final RenderRegion region;

    private ChunkRenderData data = ChunkRenderData.ABSENT;
    private CompletableFuture<?> rebuildTask = null;

    private boolean tickable;
    private boolean disposed;

    public RenderSection(RenderSectionManager manager, int id, int chunkX, int chunkY, int chunkZ, RenderRegion region) {
        this.manager = manager;

        this.id = id;
        this.region = region;

        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        this.graphicsStates = new Reference2ObjectArrayMap<>();
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

    public void deleteGraphicsState() {
        for (ChunkGraphicsState state : this.graphicsStates.values()) {
            state.delete();
        }

        this.graphicsStates.clear();
    }

    public void setData(ChunkRenderData info) {
        if (info == null) {
            throw new NullPointerException("Mesh information must not be null");
        }

        this.manager.onOcclusionDataUpdated(this, info.getOcclusionData());

        this.data = info;
        this.tickable = !info.getAnimatedSprites().isEmpty();
    }

    /**
     * @return True if the chunk render contains no data, otherwise false
     */
    public boolean isEmpty() {
        return this.graphicsStates.isEmpty() && this.data.isEmpty();
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

    public void setGraphicsState(BlockRenderPass pass, ChunkGraphicsState state) {
        this.graphicsStates.put(pass, state);
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

    public ChunkGraphicsState getGraphicsState(BlockRenderPass pass) {
        return this.graphicsStates.get(pass);
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

    public boolean isBuilt() {
        return this.data != ChunkRenderData.ABSENT;
    }

    public boolean isCulledByFrustum(FrustumIntersection frustum) {
        float x = this.getOriginX();
        float y = this.getOriginY();
        float z = this.getOriginZ();

        return !frustum.testAab(x, y, z, x + 16.0f, y + 16.0f, z + 16.0f);
    }
}
