layout(location = 0) in VertexOutput {
    vec3 color;
    float shade;

    vec2 tex_diffuse_coord;
    vec2 tex_light_coord;

    float fog_depth;
} vs_out;
