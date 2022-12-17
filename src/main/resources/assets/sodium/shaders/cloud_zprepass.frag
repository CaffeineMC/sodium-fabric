#version 450

#import <sodium:cloud.glsl>

in VertexOutput vs_out;

void main() {
    if (vs_out.color.a < 0.1) {
        discard;
    }
}
