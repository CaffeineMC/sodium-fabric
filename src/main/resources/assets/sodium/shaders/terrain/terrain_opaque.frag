#version 460 core

#import <sodium:include/terrain_fog.frag>
#import <sodium:include/terrain_buffers.frag>
#import <sodium:include/terrain_textures.frag>

in VertexOutput {
    vec3 color;
    float shade;

    vec2 tex_diffuse_coord;
    vec2 tex_light_coord;

    float fog_depth;
} vs_out;

void main() {
    vec4 frag_diffuse = texture(tex_diffuse, vs_out.tex_diffuse_coord);

#ifdef ALPHA_CUTOFF
    if (frag_diffuse.a < ALPHA_CUTOFF) {
        discard;
    }
#endif

    vec4 frag_light = texture(tex_light, vs_out.tex_light_coord);
    vec4 frag_mixed = vec4((frag_diffuse.rgb * frag_light.rgb) * vs_out.color * vs_out.shade, frag_diffuse.a);

    frag_final = _apply_fog(frag_mixed, vs_out.fog_depth, fog_color, fog_start, fog_end);
}