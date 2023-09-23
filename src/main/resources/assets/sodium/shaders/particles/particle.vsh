#version 330

#import <sodium:include/matrices.glsl>

const vec2 OFFSETS[] = vec2[](
    vec2(-1, -1),
    vec2(1, -1),
    vec2(1, 1),
    vec2(-1, 1)
);

in vec3 in_Position;
in float in_Size;
in vec2 in_TexCoord;
in vec4 in_Color;
in ivec2 in_Light;
in float in_Angle;

uniform vec4 u_CameraRotation;
uniform sampler2D u_LightTex;

out vec2 texCoord0;
out vec4 vertexColor;

// The following code is an optimized variant of Minecraft's quaternion code by Zombye and Balint.

void main() {
    gl_Position = u_ModelViewMatrix * vec4(in_Position, 1.0);
    float s = sin(in_Angle);
    float c = cos(in_Angle);
    gl_Position.xy += mat2(c,-s, s, c) * OFFSETS[gl_VertexID & 3] * in_Size;
    gl_Position = u_ProjectionMatrix * gl_Position;
    texCoord0 = in_TexCoord;
    vertexColor = in_Color * texelFetch(u_LightTex, in_Light / 16, 0);
}