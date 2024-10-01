package net.caffeinemc.mods.sodium.client.gl.arena;

public class GlBufferSegment {
    private final GlBufferArena arena;

    private boolean free = false;
    private int refCount = 1;
    private long hash;
    private boolean isHashed = false;

    private int offset;
    private int length;

    private GlBufferSegment next;
    private GlBufferSegment prev;

    public GlBufferSegment(GlBufferArena arena, int offset, int length) {
        this.arena = arena;
        this.offset = offset;
        this.length = length;
    }

    public void delete() {
        // only actually free if there's no more users
        if (--this.refCount == 0) {
            this.arena.free(this);
            this.isHashed = false;
        }
    }

    protected int getEnd() {
        return this.offset + this.length;
    }

    public int getLength() {
        return this.length;
    }

    protected void setLength(int len) {
        if (len <= 0) {
            throw new IllegalArgumentException("len <= 0");
        }

        this.length = len;
    }

    public int getOffset() {
        return this.offset;
    }

    protected void setOffset(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("start < 0");
        }

        this.offset = offset;
    }

    public void setHash(long hash) {
        this.hash = hash;
        this.isHashed = true;
    }

    public long getHash() {
        return this.hash;
    }

    public boolean isHashed() {
        return this.isHashed;
    }

    public void addRef() {
        if (this.isFree()) {
            throw new IllegalStateException("Cannot add ref to free segment");
        }
        this.refCount++;
    }

    protected void setFree(boolean free) {
        this.free = free;
        if (this.free) {
            this.refCount = 0;
        } else {
            this.refCount = Math.max(this.refCount, 1);
        }
    }

    protected boolean isFree() {
        return this.free;
    }

    protected void setNext(GlBufferSegment next) {
        this.next = next;
    }

    protected GlBufferSegment getNext() {
        return this.next;
    }

    protected GlBufferSegment getPrev() {
        return this.prev;
    }

    protected void setPrev(GlBufferSegment prev) {
        this.prev = prev;
    }

    protected void mergeInto(GlBufferSegment entry) {
        this.setLength(this.getLength() + entry.getLength());
        this.setNext(entry.getNext());

        if (this.getNext() != null) {
            this.getNext()
                    .setPrev(this);
        }
    }
}
