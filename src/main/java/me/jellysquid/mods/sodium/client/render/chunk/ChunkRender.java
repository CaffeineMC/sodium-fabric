package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChunkRender<T extends ChunkRenderState> {
    private final ColumnRender<T> column;
    private final int chunkX, chunkY, chunkZ;

    private final T renderState;
    private final Box boundingBox;

    private ChunkRenderData data = ChunkRenderData.ABSENT;
    private CompletableFuture<Void> rebuildTask = null;

    private boolean needsRebuild;
    private boolean needsImportantRebuild;

    private int rebuildFrame = -1;
    private int lastVisibleFrame = -1;

    private byte cullingState;
    private byte direction;

    public ChunkRender(ColumnRender<T> column, int chunkX, int chunkY, int chunkZ, T renderState) {
        this.renderState = renderState;
        this.column = column;

        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        int originX = chunkX << 4;
        int originY = chunkY << 4;
        int originZ = chunkZ << 4;

        this.boundingBox = new Box(originX, originY, originZ, originX + 16.0D, originY + 16.0D, originZ + 16.0D);
        this.needsRebuild = true;
    }

    public void cancelRebuildTask() {
        this.needsRebuild = false;
        this.needsImportantRebuild = false;

        if (this.rebuildTask != null) {
            this.rebuildTask.cancel(false);
            this.rebuildTask = null;
        }
    }

    public Box getBoundingBox() {
        return this.boundingBox;
    }

    public ChunkRenderData getData() {
        return this.data;
    }

    public boolean needsRebuild() {
        return this.needsRebuild;
    }

    public boolean needsImportantRebuild() {
        return this.needsRebuild && this.needsImportantRebuild;
    }

    public int getChunkY() {
        return this.chunkY;
    }

    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.data.isVisibleThrough(from, to);
    }

    public T getRenderState() {
        return this.renderState;
    }

    public void delete() {
        this.cancelRebuildTask();

        this.renderState.deleteData();
        this.setData(ChunkRenderData.ABSENT);
    }

    private void setData(ChunkRenderData info) {
        if (info == null) {
            throw new NullPointerException("Mesh information must not be null");
        }

        this.column.onChunkRenderUpdated(this.data, info);
        this.data = info;
    }

    public void scheduleRebuild(boolean important) {
        this.needsImportantRebuild = important;
        this.needsRebuild = true;
    }

    public void upload(ChunkRenderData meshInfo) {
        this.renderState.uploadData(meshInfo.getMeshes());
        this.setData(meshInfo);
    }

    public void finishRebuild() {
        this.needsRebuild = false;
        this.needsImportantRebuild = false;
    }

    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    public void updateCullingState(byte parent, Direction from) {
        this.cullingState = (byte) (parent | (1 << from.ordinal()));
    }

    public boolean canCull(Direction from) {
        return (this.cullingState & 1 << from.ordinal()) > 0;
    }

    public void resetGraphState() {
        this.direction = -1;
        this.cullingState = 0;
    }

    public void setRebuildFrame(int frame) {
        this.rebuildFrame = frame;
    }

    public void setLastVisibleFrame(int frame) {
        this.lastVisibleFrame = frame;
    }

    public void setDirection(Direction dir) {
        this.direction = (byte) dir.ordinal();
    }

    public int getRebuildFrame() {
        return this.rebuildFrame;
    }

    public ChunkSectionPos getChunkPos() {
        return ChunkSectionPos.from(this.chunkX, this.chunkY, this.chunkZ);
    }

    public ColumnRender<T> getColumn() {
        return this.column;
    }

    public boolean isVisible(Frustum frustum, int frame) {
        return this.column.isVisible(frustum, frame) && frustum.isVisible(this.boundingBox);
    }

    public void tickTextures() {
        List<Sprite> sprites = this.getData().getAnimatedSprites();

        if (!sprites.isEmpty()) {
            int size = sprites.size();

            // We would like to avoid allocating an iterator here
            // noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < size; i++) {
                SpriteUtil.ensureSpriteReady(sprites.get(i));
            }
        }
    }

    public int getOriginX() {
        return this.chunkX << 4;
    }

    public int getOriginY() {
        return this.chunkY << 4;
    }

    public int getOriginZ() {
        return this.chunkZ << 4;
    }

    public boolean isWithinDistance(Vec3d pos, int distance) {
        return this.getSquaredDistance(pos.x, pos.y, pos.z) <= distance;
    }

    public double getSquaredDistance(BlockPos pos) {
        return this.getSquaredDistance(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    public double getSquaredDistance(double x, double y, double z) {
        double xDist = x - this.getCenterX();
        double yDist = y - this.getCenterY();
        double zDist = z - this.getCenterZ();

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }

    private double getCenterX() {
        return this.getOriginX() + 8.0D;
    }

    private double getCenterY() {
        return this.getOriginY() + 8.0D;
    }

    private double getCenterZ() {
        return this.getOriginZ() + 8.0D;
    }

    public byte getCullingState() {
        return this.cullingState;
    }

    public Direction getDirection() {
        if (this.direction < 0) {
            return null;
        }

        return DirectionUtil.ALL_DIRECTIONS[this.direction];
    }

    public int getLastVisibleFrame() {
        return this.lastVisibleFrame;
    }
}
