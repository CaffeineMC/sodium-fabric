package net.caffeinemc.sodium.render.buffer;

public record VertexRange(int firstVertex, int vertexCount) {
    public static final long NULL = pack(-1, -1);

    public static long pack(int firstVertex, int vertexCount) {
        return ((long) firstVertex & 0xffffffffL) | (((long) vertexCount & 0xffffffffL) << 32);
    }
    
    public static int unpackFirstVertex(long packed) {
        return (int) (packed & 0xffffffffL);
    }
    
    public static int unpackVertexCount(long packed) {
        return (int) ((packed >>> 32) & 0xffffffffL);
    }
}
