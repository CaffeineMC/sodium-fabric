#version 450

#import <sodium:cloud.glsl>

layout(location = 0) in vec3 in_pos;
layout(location = 1) in vec4 in_color;

out VertexOutput vs_out;

void main() {
    gl_Position = mat_proj * mat_modelview * vec4(in_pos, 1.0);

    vec4 pos = mat_modelview * vec4(in_pos.x, 0.0, in_pos.z, 1.0);
    vs_out.distance = length(pos.xyz);
    vs_out.color = in_color * color_mod;
}