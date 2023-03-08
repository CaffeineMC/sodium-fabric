package me.jellysquid.mods.sodium.client.render.chunk.graph;

public class ChunkGraphIterationQueue extends AbstractWrappingQueue {
    private final byte[] packed;

    public ChunkGraphIterationQueue() {
        super(4096);

        this.packed = new byte[this.capacity() * 3];
    }

    public void add(int x, int y, int z) {
        var index = this.reserveNext();

        this.packed[(index * 3) + 0] = (byte) x;
        this.packed[(index * 3) + 1] = (byte) y;
        this.packed[(index * 3) + 2] = (byte) z;
    }

    @Override
    protected void erase(int index) {

    }

    public int getPositionX(int index) {
        return this.packed[(index * 3) + 0];
    }

    public int getPositionY(int index) {
        return this.packed[(index * 3) + 1];
    }

    public int getPositionZ(int index) {
        return this.packed[(index * 3) + 2];
    }
}
