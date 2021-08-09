package me.jellysquid.mods.sodium.client.gl.arena;

class PendingBufferCopyCommand {
    public final int readOffset;
    public final int writeOffset;

    public int length;

    PendingBufferCopyCommand(int readOffset, int writeOffset, int length) {
        this.readOffset = readOffset;
        this.writeOffset = writeOffset;
        this.length = length;
    }
}
