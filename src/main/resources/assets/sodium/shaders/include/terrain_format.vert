// The position of the vertex around the model origin
vec3 _vert_position;

// The block texture coordinate of the vertex
vec2 _vert_tex_diffuse_coord;

// The light texture coordinate of the vertex
vec2 _vert_tex_light_coord;

// The color (rgb) and shade (a) of the vertex
vec4 _vert_color_shade;

layout(location = 0) in vec3 in_position;
layout(location = 1) in vec4 in_color;
layout(location = 2) in vec2 in_tex_diffuse_coord;
layout(location = 3) in vec2 in_tex_light_coord;

void _vert_init() {
    _vert_position = (in_position * 16.0f) + 8.0f;
    _vert_tex_diffuse_coord = in_tex_diffuse_coord;
    _vert_tex_light_coord = in_tex_light_coord;
    _vert_color_shade = in_color;
}