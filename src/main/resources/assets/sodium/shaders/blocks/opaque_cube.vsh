#import <sodium:blocks/base.vsh>

struct Quad {
    // offset 0
    // Block position (XYZ) and face index (W) of the quad
    uint position_id;

    // offset 4
    // UV-coordinates of the light texture (Unorm4x8)
    uint tex_light_block;
    uint tex_light_sky;

    // offset 12
    // UV-coordinates of the diffuse texture (Unorm2x16)
    uint tex_diffuse_min;
    uint tex_diffuse_max;
};

struct BlockVertex {
    uint tint;
};

struct Uniforms {
    uint quad_offset;
    uint vertex_offset;
    vec4 offset;
};

layout(std430, binding = 0) readonly buffer ssbo_Quads {
    Quad quads[];
};

layout(std430, binding = 1) readonly buffer ssbo_Vertices {
    BlockVertex vertices[];
};

layout(std140, binding = 0) uniform ubo_InstanceUniforms {
    Uniforms instanceUniforms[MAX_DRAWS];
};

const uint FACE_COUNT = 6;
const uint VERTICES_PER_FACE = 4;

const vec3[FACE_COUNT][VERTICES_PER_FACE] CUBE_VERTICES = {
    { vec3(0, 0, 0), vec3(1, 0, 0), vec3(1, 0, 1), vec3(0, 0, 1) }, /* Down */
    { vec3(0, 1, 0), vec3(0, 1, 1), vec3(1, 1, 1), vec3(1, 1, 0) }, /* Up */

    { vec3(1, 1, 0), vec3(1, 0, 0), vec3(0, 0, 0), vec3(0, 1, 0) }, /* North */
    { vec3(0, 1, 1), vec3(0, 0, 1), vec3(1, 0, 1), vec3(1, 1, 1) }, /* South */

    { vec3(0, 1, 0), vec3(0, 0, 0), vec3(0, 0, 1), vec3(0, 1, 1) }, /* West */
    { vec3(1, 1, 1), vec3(1, 0, 1), vec3(1, 0, 0), vec3(1, 1, 0) }, /* East */
};

const vec2[VERTICES_PER_FACE] TEXTURE_MAP_MIN = {
    vec2(0, 1), vec2(0, 0),
    vec2(1, 0), vec2(1, 1),
};
const vec2[VERTICES_PER_FACE] TEXTURE_MAP_MAX = {
    vec2(1, 0), vec2(1, 1),
    vec2(0, 1), vec2(0, 0),
};

Vertex _get_vertex(Uniforms uniforms) {
    uint vertex_index = _get_vertex_index();

    uint corner_index = vertex_index % 4u;
    uint quad_index = vertex_index / 4u;

    Quad quad = quads[uniforms.quad_offset + quad_index];
    vec3 block_position = vec3((quad.position_id & 0xFF000000u) >> 24u, (quad.position_id & 0x00FF0000u) >> 16u, (quad.position_id & 0x0000FF00u) >> 8u);
    vec3 position = block_position + CUBE_VERTICES[quad.position_id & 0x000000FFu][corner_index];
    vec2 tex_light_coord = vec2(unpackUnorm4x8(quad.tex_light_block)[corner_index],
        unpackUnorm4x8(quad.tex_light_sky)[corner_index]);
    vec2 tex_diffuse_coord = (unpackUnorm2x16(quad.tex_diffuse_min) * TEXTURE_MAP_MIN[corner_index]) +
        (unpackUnorm2x16(quad.tex_diffuse_max) * TEXTURE_MAP_MAX[corner_index]);

    BlockVertex vertex = vertices[uniforms.vertex_offset + vertex_index];
    vec4 color_and_shade = unpackUnorm4x8(vertex.tint);

    return Vertex(position, tex_diffuse_coord, tex_light_coord, color_and_shade);
}

void main() {
    Uniforms uniforms = instanceUniforms[_get_instance_index()];
    Vertex vertex = _get_vertex(uniforms);

    _emit_vertex(vertex, uniforms.offset.xyz);
}