#version 330

#define COLOR_SCALE 1.0 / 255.0
#define PARTICLE_STRIDE 11

#import <sodium:include/matrices.glsl>

const vec2 OFFSETS[] = vec2[](
    vec2(-1, -1),
    vec2(1, -1),
    vec2(1, 1),
    vec2(-1, 1)
);

const int INDICES[] = int[](
    0, 1, 2,
    0, 2, 3
);

uniform sampler2D u_LightTex;
uniform usamplerBuffer u_BufferTexture; // R_32UI

out vec2 texCoord0;
out vec4 vertexColor;

// 11 x 4 = 44 bytes stride
vec3 position;
float size;
vec4 color;
ivec2 light;
float angle;
vec2 minTexUV;
vec2 maxTexUV;

// Returns a collection of 4 bytes
// ptr is essentially multiplied by 4 since u_BufferTexture is R_32UI
uint readBuffer(int ptr) {
    return texelFetch(u_BufferTexture, ptr).x;
}

float readBufferF(int ptr) {
    return uintBitsToFloat(readBuffer(ptr));
}

vec3 readBufferPos(int ptr) {
    return uintBitsToFloat(uvec3(readBuffer(ptr), readBuffer(ptr + 1), readBuffer(ptr + 2)));
}

vec4 readBufferColor(int ptr) {
    return vec4((uvec4(readBuffer(ptr)) >> uvec4(0, 8, 16, 24)) & uvec4(0xFFu)) * COLOR_SCALE;
}

ivec2 readBufferLight(int ptr) {
    return ivec2((uvec2(readBuffer(ptr)) >> uvec2(0, 16)) & uvec2(0xFFFFu));
}

vec2 readBufferTex(int ptr) {
    return vec2(readBufferF(ptr), readBufferF(ptr + 1));
}

void init() {
    int base = PARTICLE_STRIDE * (gl_VertexID / 6);

    position = readBufferPos(base);
    size = readBufferF(base + 3);
    color = readBufferColor(base + 4);
    light = readBufferLight(base + 5);
    angle = readBufferF(base + 6);
    minTexUV = readBufferTex(base + 7);
    maxTexUV = readBufferTex(base + 9);
}

void main() {
    init();
    int vertexIndex = INDICES[gl_VertexID % 6];
    vec2 texUVs[] = vec2[](
        maxTexUV,
        vec2(maxTexUV.x, minTexUV.y),
        minTexUV,
        vec2(minTexUV.x, maxTexUV.y)
    );

    // The following code is an optimized variant of Minecraft's quaternion code by Zombye and Balint.
    gl_Position = u_ModelViewMatrix * vec4(position, 1.0);
    float s = sin(angle);
    float c = cos(angle);
    gl_Position.xy += mat2(c,-s, s, c) * OFFSETS[vertexIndex] * size;
    gl_Position = u_ProjectionMatrix * gl_Position;

    texCoord0 = texUVs[vertexIndex];

    vertexColor = color * texelFetch(u_LightTex, light / 16, 0);
}