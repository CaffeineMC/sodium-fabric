package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.common.util.collections.TrackedArrayItem;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.concurrent.CompletableFuture;

/**
 * The render state object for a chunk section. This contains all the graphics state for each render pass along with
 * data about the render in the chunk visibility graph.
 */
public class ChunkRenderContainer implements TrackedArrayItem {
    private final SodiumWorldRenderer worldRenderer;
    private final int chunkX, chunkY, chunkZ;
    private final int id;

    private final ChunkRenderBackend backend;
    private final ChunkGraphicsStateArray graphicsStates;
    private final ChunkRenderColumn column;

    private ChunkRenderData data = ChunkRenderData.ABSENT;
    private CompletableFuture<Void> rebuildTask = null;

    private boolean needsRebuild;
    private boolean needsImportantRebuild;

    private boolean tickable;

    public ChunkRenderContainer(ChunkRenderBackend backend, SodiumWorldRenderer worldRenderer, int chunkX, int chunkY, int chunkZ, ChunkRenderColumn column, int id) {
        this.backend = backend;
        this.worldRenderer = worldRenderer;

        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        this.graphicsStates = new ChunkGraphicsStateArray(BlockRenderPass.COUNT);
        this.column = column;

        this.id = id;
    }

    /**
     * Cancels any pending tasks to rebuild the chunk. If the result of any pending tasks has not been processed yet,
     * those will also be discarded when processing finally happens.
     */
    public void cancelRebuildTask() {
        this.needsRebuild = false;
        this.needsImportantRebuild = false;

        if (this.rebuildTask != null) {
            this.rebuildTask.cancel(false);
            this.rebuildTask = null;
        }
    }

    public ChunkRenderData getData() {
        return this.data;
    }

    /**
     * @return True if the render's state is out of date with the world state
     */
    public boolean needsRebuild() {
        return this.needsRebuild;
    }

    /**
     * @return True if the render's rebuild should be performed as blocking
     */
    public boolean needsImportantRebuild() {
        return this.needsImportantRebuild;
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
    }

    private void deleteGraphicsState() {
        for (int i = 0; i < this.graphicsStates.getSize(); i++) {
            this.backend.deleteGraphicsState(this.graphicsStates.getValue(i));
        }

        this.graphicsStates.clear();
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
     * Marks this render as needing an update. Important updates are scheduled as "blocking" and will prevent the next
     * frame from being rendered until the update is performed.
     * @param important True if the update is blocking, otherwise false
     */
    public boolean scheduleRebuild(boolean important) {
        boolean changed = !this.needsRebuild || (!this.needsImportantRebuild && important);

        this.needsImportantRebuild = important;
        this.needsRebuild = true;

        return changed;
    }

    /**
     * @return True if the chunk render contains no data, otherwise false
     */
    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    /**
     * Returns the chunk section position which this render refers to in the world.
     */
    public ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
    }

    /**
     * Ensures that all resources attached to the given chunk render are "ticked" forward. This should be called every
     * time before this render is drawn if {@link ChunkRenderContainer#isTickable()} is true.
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

    public int getRenderX() {
        return this.getOriginX() - 8;
    }

    public int getRenderY() {
        return this.getOriginY() - 8;
    }

    public int getRenderZ() {
        return this.getOriginZ() - 8;
    }

    /**
     * @return The squared distance from the center of this chunk in the world to the given position
     */
    public double getSquaredDistance(float x, float y, float z) {
        float xDist = x - this.getCenterX();
        float yDist = y - this.getCenterY();
        float zDist = z - this.getCenterZ();

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }

    /**
     * @return The x-coordinate of the center position of this chunk render
     */
    private float getCenterX() {
        return this.getOriginX() + 8.0F;
    }

    /**
     * @return The y-coordinate of the center position of this chunk render
     */
    private float getCenterY() {
        return this.getOriginY() + 8.0F;
    }

    /**
     * @return The z-coordinate of the center position of this chunk render
     */
    private float getCenterZ() {
        return this.getOriginZ() + 8.0F;
    }

    public BlockPos getRenderOrigin() {
        return new BlockPos(this.getRenderX(), this.getRenderY(), this.getRenderZ());
    }

    public ChunkGraphicsStateArray getGraphicsStates() {
        return this.graphicsStates;
    }

    /**
     * @return The squared distance from the center of this chunk in the world to the given position
     */
    public double getSquaredDistanceXZ(float x, float z) {
        float xDist = x - this.getCenterX();
        float zDist = z - this.getCenterZ();

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

    public int getFacesWithData() {
        return this.data.getFacesWithData();
    }

    public boolean canRebuild() {
        return this.column.areNeighborsPresent();
    }

    @Override
    public int getId() {
        return this.id;
    }
}
