package me.jellysquid.mods.sodium.client.render.chunk.graph;

public class ChunkGraphIterationQueue extends AbstractWrappingQueue {
    private final byte[] packed;

    public ChunkGraphIterationQueue() {
        super(2048); // TODO: this need to be really large right now, because the queue becomes corrupted when it resizes

        this.packed = new byte[this.capacity() * 4];
    }

    public void add(int x, int y, int z, int dir) {
        var index = this.reserveNext();

        this.packed[(index * 4) + 0] = (byte) x;
        this.packed[(index * 4) + 1] = (byte) y;
        this.packed[(index * 4) + 2] = (byte) z;
        this.packed[(index * 4) + 3] = (byte) dir;
    }

    @Override
    protected void erase(int index) {

    }

    public int getPositionX(int index) {
        return this.packed[(index * 4) + 0];
    }

    public int getPositionY(int index) {
        return this.packed[(index * 4) + 1];
    }

    public int getPositionZ(int index) {
        return this.packed[(index * 4) + 2];
    }

    public int getDirection(int index) {
        return this.packed[(index * 4) + 3];
    }

}
