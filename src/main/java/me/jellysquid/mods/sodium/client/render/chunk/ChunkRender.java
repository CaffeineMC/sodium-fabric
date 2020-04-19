package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.FrustumExtended;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.Vector3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ChunkRender<T extends ChunkRenderState> {
    private final ColumnRender<T> column;
    private final int chunkX, chunkY, chunkZ;

    private final T[] renderState;

    private ChunkRenderData data = ChunkRenderData.ABSENT;
    private CompletableFuture<Void> rebuildTask = null;

    private boolean needsRebuild;
    private boolean needsImportantRebuild;

    private int rebuildFrame = -1;
    private int lastVisibleFrame = -1;

    private byte cullingState;
    private byte direction;

    private final float boundsMinX;
    private final float boundsMinY;
    private final float boundsMinZ;

    private final float boundsMaxX;
    private final float boundsMaxY;
    private final float boundsMaxZ;

    public ChunkRender(ChunkRenderBackend<T> backend, ColumnRender<T> column, int chunkX, int chunkY, int chunkZ) {
        this.column = column;

        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.chunkZ = chunkZ;

        int originX = chunkX << 4;
        int originY = chunkY << 4;
        int originZ = chunkZ << 4;

        this.boundsMinX = originX;
        this.boundsMinY = originY;
        this.boundsMinZ = originZ;
        this.boundsMaxX = originX + 16.0f;
        this.boundsMaxY = originY + 16.0f;
        this.boundsMaxZ = originZ + 16.0f;

        //noinspection unchecked
        this.renderState = (T[]) Array.newInstance(backend.getRenderStateType(), BlockRenderPass.count());

        if (column.isSectionEmpty(chunkY)) {
            this.setData(ChunkRenderData.EMPTY);
        } else {
            this.scheduleRebuild(false);
        }
    }

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

    public boolean needsRebuild() {
        return this.needsRebuild;
    }

    public boolean needsImportantRebuild() {
        return this.needsImportantRebuild;
    }

    public int getChunkY() {
        return this.chunkY;
    }

    public boolean isVisibleThrough(Direction from, Direction to) {
        return this.data.isVisibleThrough(from, to);
    }

    public void setRenderState(BlockRenderPass pass, T data) {
        this.renderState[pass.ordinal()] = data;
    }

    public void delete() {
        this.cancelRebuildTask();
        this.setData(ChunkRenderData.ABSENT);
        this.resetRenderStates();
    }

    public void setData(ChunkRenderData info) {
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

    public boolean isVisible(FrustumExtended frustum) {
        return frustum.fastAabbTest(this.boundsMinX, this.boundsMinY, this.boundsMinZ, this.boundsMaxX, this.boundsMaxY, this.boundsMaxZ);
    }

    public boolean isTickable() {
        return this.getData().getAnimatedSprites().isEmpty();
    }

    public void tick() {
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

    public Vector3d getTranslation() {
        return new Vector3d(this.getOriginX(), this.getOriginY(), this.getOriginZ());
    }

    public void resetRenderStates() {
        for (T state : this.renderState) {
            if (state != null) {
                state.delete();
            }
        }

        Arrays.fill(this.renderState, null);
    }

    public T[] getRenderStates() {
        return this.renderState;
    }

    public boolean hasData() {
        return this.data != ChunkRenderData.EMPTY;
    }

    public boolean canTick() {
        return !this.getData().getAnimatedSprites().isEmpty();
    }

    public BlockPos getOrigin() {
        return new BlockPos(this.getOriginX(), this.getOriginY(), this.getOriginZ());
    }
}
