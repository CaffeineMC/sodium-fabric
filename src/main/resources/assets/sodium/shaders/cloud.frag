#version 450

#import <sodium:cloud.glsl>

in VertexOutput vs_out;

layout(location = 0) out vec4 frag_final;

float linear_fog_fade(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) {
        return 1.0;
    } else if (vertexDistance >= fogEnd) {
        return 0.0;
    }

    return smoothstep(fogEnd, fogStart, vertexDistance);
}

void main() {
    float width = fog_end - fog_start;
    float fade = linear_fog_fade(vs_out.distance, fog_start, fog_start + (width * 4.0));
    frag_final = vec4(vs_out.color.rgb, vs_out.color.a * fade);
}
