package me.jellysquid.mods.sodium.client.render.chunk.graph;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;

import java.util.Arrays;

public class ChunkGraphIterationQueue extends AbstractWrappingQueue {
    private RenderSection[] arraySection;
    private byte[] arrayDirection;
    private byte[] arrayCull;

    public ChunkGraphIterationQueue() {
        super(128);

        this.arraySection = new RenderSection[this.capacity()];
        this.arrayDirection = new byte[this.capacity()];
        this.arrayCull = new byte[this.capacity()];
    }

    public RenderSection getSection() {
        return this.arraySection[this.currentElementIndex()];
    }

    public int getDirection() {
        return this.arrayDirection[this.currentElementIndex()];
    }

    public int getCullState() {
        return this.arrayCull[this.currentElementIndex()];
    }

    public void add(RenderSection section, int dir, int cull) {
        var index = this.reserveNext();

        this.arraySection[index] = section;
        this.arrayDirection[index] = (byte) dir;
        this.arrayCull[index] = (byte) cull;
    }

    @Override
    protected void erase(int index) {
        this.arraySection[index] = null;
        this.arrayDirection[index] = -1;
        this.arrayCull[index] = -1;
    }

    @Override
    protected void resize(int capacity) {
        this.arraySection = Arrays.copyOf(this.arraySection, capacity);
        this.arrayDirection = Arrays.copyOf(this.arrayDirection, capacity);
        this.arrayCull = Arrays.copyOf(this.arrayCull, capacity);
    }
}
