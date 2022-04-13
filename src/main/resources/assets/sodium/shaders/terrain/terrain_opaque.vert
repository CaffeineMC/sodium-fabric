#version 460 core

#import <sodium:include/terrain_fog.vert>
#import <sodium:include/terrain_draw.vert>
#import <sodium:include/terrain_view.vert>
#import <sodium:include/terrain_format.vert>

out VertexOutput {
    vec3 color;
    float shade;

    vec2 tex_diffuse_coord;
    vec2 tex_light_coord;

    float fog_depth;
} vs_out;

void main() {
    _vert_init();

    // Local space -> View space
    vec3 view_position = _apply_view_transform(_vert_position);

    // View space -> Clip space
    gl_Position = mat_modelviewproj * vec4(view_position, 1.0);

    // Unpack the vertex color and shade values
    vs_out.color = _vert_color_shade.rgb;
    vs_out.shade = _vert_color_shade.a;

    // Pass the texture coordinates verbatim
    vs_out.tex_diffuse_coord = _vert_tex_diffuse_coord;
    vs_out.tex_light_coord = _vert_tex_light_coord;

    // The distance of the vertex from the camera is just the view-space coordinate
    vs_out.fog_depth = _get_fog_depth(view_position);
}
