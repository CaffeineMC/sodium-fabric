#version 450 core

#import <sodium:include/terrain_draw.vert>
#import <sodium:include/terrain_fog.vert>
#import <sodium:include/terrain_view.vert>
#import <sodium:include/terrain_format.vert>
#import <sodium:include/terrain_light.vert>
#import <sodium:include/terrain_textures.glsl>
#import <sodium:terrain/terrain_opaque.glsl>

out VertexOutput vs_out;

void main() {
    _vert_init();

    // Local space -> View space
    vec3 view_position = _apply_view_transform(_vert_position);

    // View space -> Clip space
    gl_Position = mat_modelviewproj * vec4(view_position, 1.0);

    // Apply the lightmap to the color and shade
    vs_out.color_shade = _vert_color_shade * _sample_lightmap(tex_light, _vert_tex_light_coord);

    // Pass the texture coordinates verbatim
    vs_out.tex_diffuse_coord = _vert_tex_diffuse_coord;

    // The distance of the vertex from the camera is just the view-space coordinate
    vs_out.fog_depth = _get_fog_depth(view_position);
}
