#import <sodium:blocks/base.vsh>

struct BlockVertex {
    float position_x;
    float position_y;
    float position_z;
    uint color;
    uint tex_diffuse;
    uint tex_light;
};

struct Uniforms {
    uint vertex_offset;
    vec4 offset;
};

layout(std430, binding = 0) readonly buffer ssbo_Vertices {
    BlockVertex vertices[];
};

layout(std140, binding = 0) uniform ubo_InstanceUniforms {
    Uniforms instanceUniforms[MAX_DRAWS];
};

Vertex _get_vertex(Uniforms uniforms) {
    BlockVertex vert = vertices[uniforms.vertex_offset + _get_vertex_index()];

    vec3 position = vec3(vert.position_x, vert.position_y, vert.position_z);
    vec4 color_and_shade = unpackUnorm4x8(vert.color);
    vec2 tex_diffuse_coord = unpackUnorm2x16(vert.tex_diffuse);
    vec2 tex_light_coord = unpackUnorm2x16(vert.tex_light);

    return Vertex(position, tex_diffuse_coord, tex_light_coord, color_and_shade);
}

void main() {
    Uniforms uniforms = instanceUniforms[_get_instance_index()];
    Vertex vertex = _get_vertex(uniforms);

    _emit_vertex(vertex, uniforms.offset.xyz);
}