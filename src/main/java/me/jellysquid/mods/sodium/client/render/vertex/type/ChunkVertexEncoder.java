package me.jellysquid.mods.sodium.client.render.vertex.type;

public interface ChunkVertexEncoder {
    long write(long ptr, Vertex vertex, int chunkId);

    class Vertex {
        public float x;
        public float y;
        public float z;
        public int color;
        public float u;
        public float v;
        public int light;

        public static Vertex[] uninitializedQuad() {
            Vertex[] vertices = new Vertex[4];

            for (int i = 0; i < 4; i++) {
                vertices[i] = new Vertex();
            }

            return vertices;
        }
    }
}
