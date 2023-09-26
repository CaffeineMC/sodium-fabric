#version 330

#define COLOR_SCALE 1.0 / 255.0
#define PARTICLE_STRIDE 7

#import <sodium:include/matrices.glsl>

const vec2 OFFSETS[] = vec2[](
    vec2(-1, -1),
    vec2(1, -1),
    vec2(1, 1),
    vec2(-1, 1)
);

in vec2 in_TexCoord;

uniform sampler2D u_LightTex;
uniform usamplerBuffer u_BufferTexture; // R_32UI

out vec2 texCoord0;
out vec4 vertexColor;

// 7 x 4 = 28 bytes stride
vec3 position;
float size;
vec4 color;
ivec2 light;
float angle;

// Returns a collection of 4 bytes
// ptr is essentially multiplied by 4 since u_BufferTexture is R_32UI
uint readBuffer(uint ptr) {
    return texelFetch(u_BufferTexture, ptr).x;
}

float readBufferF(uint ptr) {
    return uintBitsToFloat(readBuffer(ptr));
}

vec3 readBufferPos(uint ptr) {
    return uintBitsToFloat(uvec3(readBuffer(ptr), readBuffer(ptr + 1), readBuffer(ptr + 2)));
}

vec4 readBufferColor(uint ptr) {
    return vec4((uvec4(readBuffer(ptr)) >> uvec4(0, 8, 16, 24)) & uvec4(0xFFu)) * COLOR_SCALE;
}

ivec2 readBufferLight(uint ptr) {
    return ivec2((uvec2(readBuffer(ptr)) >> uvec2(0, 16)) & uvec2(0xFFFFu));
}

void init() {
    uint base = PARTICLE_STRIDE * (gl_VertexID >> 2);

    position = readBufferPos(base);
    size = readBufferF(base + 3);
    color = readBufferColor(base + 4);
    light = readBufferLight(base + 5);
    angle = readBufferF(base + 6);
}

// The following code is an optimized variant of Minecraft's quaternion code by Zombye and Balint.
void main() {
    init();

    gl_Position = u_ModelViewMatrix * vec4(position, 1.0);
    float s = sin(angle);
    float c = cos(angle);
    gl_Position.xy += mat2(c,-s, s, c) * OFFSETS[gl_VertexID & 3] * size;
    gl_Position = u_ProjectionMatrix * gl_Position;
    texCoord0 = in_TexCoord;
    vertexColor = color * texelFetch(u_LightTex, light / 16, 0);
}