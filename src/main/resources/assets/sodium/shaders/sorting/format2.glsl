uniform uint baseVertexOffset;
uniform vec3 camera;

struct Vertex {
    int a;
    int b;
    int c;
    int d;
    int e;
};

layout(std430, binding = 1) buffer VertexBuffer {
    Vertex verticies[];
};

float unpackVertexPos(uint s) {
    return (float(s) * (32f/65536f))  - 8.0f;
}

float getDistance(uint quadId) {
    QuadIndicies i = indicies[quadId+baseIndexOffset];
    uint idA = (i.a&0xFFFF)+baseVertexOffset;
    uint idB = (i.b&0xFFFF)+baseVertexOffset;
    Vertex va = verticies[idA];
    Vertex vb = verticies[idB];
    vec3 pos = vec3(unpackVertexPos((va.a)&0xFFFF),unpackVertexPos((va.a>>16)&0xFFFF),unpackVertexPos((va.b)&0xFFFF));
    pos += vec3(unpackVertexPos((vb.a)&0xFFFF),unpackVertexPos((vb.a>>16)&0xFFFF),unpackVertexPos((vb.b)&0xFFFF));
    pos /= 2;

    vec3 delta = abs(pos-camera);
    return delta.x+delta.y+delta.z;//delta.y;//
}

void swapQuads(uint quadIdA, uint quadIdB) {
    QuadIndicies a = indicies[quadIdA+baseIndexOffset];
    indicies[quadIdA+baseIndexOffset] = indicies[quadIdB+baseIndexOffset];
    indicies[quadIdB+baseIndexOffset] = a;
}

uint getQuadCount() {
    return quadCount;
}
