package me.jellysquid.mods.sodium.client.render.chunk;

public abstract class ChunkGraphicsState {
    private final int x, y, z;

    protected ChunkGraphicsState(ChunkRenderContainer<?> container) {
        this.x = container.getRenderX();
        this.y = container.getRenderY();
        this.z = container.getRenderZ();
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
}
