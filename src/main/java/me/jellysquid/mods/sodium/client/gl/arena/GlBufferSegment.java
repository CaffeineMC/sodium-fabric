package me.jellysquid.mods.sodium.client.gl.arena;

public class GlBufferSegment {
    private final GlBufferArena arena;
    private final int offset;
    private final int count;

    GlBufferSegment(GlBufferArena arena, int offset, int count) {
        this.arena = arena;

        this.offset = offset;
        this.count = count;
    }

    public int getElementOffset() {
        return this.offset;
    }

    public int getElementCount() {
        return this.count;
    }

    public void delete() {
        this.arena.free(this);
    }
}
