#version 330

#import <sodium:include/matrices.glsl>

const vec2[] OFFSETS = vec2[](
    vec2(-1.0, -1.0),
    vec2(-1.0,  1.0),
    vec2( 1.0,  1.0),
    vec2( 1.0, -1.0)
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

void main() {
    int vertexId = gl_VertexID & 3;
    vec2 pos = OFFSETS[vertexId];

    vec4 q0;
    if (in_Angle == 0.0) {
        q0 = u_CameraRotation;
    } else {
        float sin = sin(in_Angle * 0.5);
        float cos = cos(in_Angle * 0.5);
        q0 = vec4(
            u_CameraRotation.x * cos + u_CameraRotation.y * sin,
            u_CameraRotation.y * cos - u_CameraRotation.x * sin,
            u_CameraRotation.w * sin + u_CameraRotation.z * cos,
            u_CameraRotation.w * cos - u_CameraRotation.z * sin
        );
    }

    vec4 q2 = vec4(-q0.xyz, q0.w);
    vec4 q1 = vec4(
        (q0.w * pos.x) - (q0.z * pos.y),
        (q0.w * pos.y) + (q0.z * pos.x),
       -(q0.x * pos.x) - (q0.y * pos.y),
        (q0.x * pos.y) - (q0.y * pos.x)
    );

    vec3 q3 = vec3(
        q1.z * q2.x + q1.x * q2.w + q1.y * q2.z - q1.w * q2.y,
        q1.z * q2.y - q1.x * q2.z + q1.y * q2.w + q1.w * q2.x,
        q1.z * q2.z + q1.x * q2.y - q1.y * q2.x + q1.w * q2.w
    );

    vec3 f = q3 * in_Size + in_Position;

    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(f, 1.0);
    texCoord0 = in_TexCoord;
    vertexColor = in_Color * texelFetch(u_LightTex, in_Light / 16, 0);
}
