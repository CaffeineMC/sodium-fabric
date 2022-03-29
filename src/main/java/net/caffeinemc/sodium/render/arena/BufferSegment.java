package net.caffeinemc.sodium.render.arena;

public class BufferSegment {
    private final BufferArena arena;

    private boolean free = false;

    private int offset;
    private int length;

    private BufferSegment next;
    private BufferSegment prev;

    public BufferSegment(BufferArena arena, int offset, int length) {
        this.arena = arena;
        this.offset = offset;
        this.length = length;
    }

    public void delete() {
        this.arena.free(this);
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

    protected void setFree(boolean free) {
        this.free = free;
    }

    protected boolean isFree() {
        return this.free;
    }

    protected void setNext(BufferSegment next) {
        this.next = next;
    }

    protected BufferSegment getNext() {
        return this.next;
    }

    protected BufferSegment getPrev() {
        return this.prev;
    }

    protected void setPrev(BufferSegment prev) {
        this.prev = prev;
    }

    protected void mergeInto(BufferSegment entry) {
        this.setLength(this.getLength() + entry.getLength());
        this.setNext(entry.getNext());

        if (this.getNext() != null) {
            this.getNext()
                    .setPrev(this);
        }
    }
}
