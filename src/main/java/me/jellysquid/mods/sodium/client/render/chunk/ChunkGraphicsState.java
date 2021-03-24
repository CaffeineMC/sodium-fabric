package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.common.util.collections.TrackedArrayItem;

public abstract class ChunkGraphicsState implements TrackedArrayItem {
    private final int id;
    private final int x, y, z;

    protected ChunkGraphicsState(ChunkRenderContainer container, int id) {
        this.x = container.getRenderX();
        this.y = container.getRenderY();
        this.z = container.getRenderZ();
        this.id = id;
    }

    public abstract void delete();

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    @Override
    public int getId() {
        return this.id;
    }
}
