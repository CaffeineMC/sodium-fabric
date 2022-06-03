package net.caffeinemc.sodium.render.arena;

class PendingResizeCopy {
    public final int readOffset;
    public final int writeOffset;

    public int length;

    PendingResizeCopy(int readOffset, int writeOffset, int length) {
        this.readOffset = readOffset;
        this.writeOffset = writeOffset;
        this.length = length;
    }
}
