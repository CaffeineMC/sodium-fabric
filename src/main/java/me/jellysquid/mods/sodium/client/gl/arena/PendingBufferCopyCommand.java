package me.jellysquid.mods.sodium.client.gl.arena;

class PendingBufferCopyCommand {
    public final long readOffset;
    public final long writeOffset;

    public long length;

    PendingBufferCopyCommand(long readOffset, long writeOffset, long length) {
        this.readOffset = readOffset;
        this.writeOffset = writeOffset;
        this.length = length;
    }
}
