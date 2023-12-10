#version 450 core

#define MODEL_SCALE        32.0 / 65536.0
#define MODEL_ORIGIN       8.0

#define COLOR_SCALE        1.0 / 255.0

#define TEX_COORD_SCALE    1.0 / 65536.0

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_matrices.glsl>
#import <sodium:include/chunk_material.glsl>

uniform int u_FogShape;
uniform vec3 u_RegionOffset;

struct Quad {
    uvec3 position_hi;    // offset: 0    size: 16
    uvec3 position_lo;    // offset: 16   size: 16

    uvec4 color;          // offset: 32   size: 16

    uvec2 tex_diffuse_hi; // offset: 48   size:  8
    uvec2 tex_diffuse_lo; // offset: 56   size:  8

    uvec2 light;          // offset: 64   size:  8

    uint material;        // offset: 72   size:  4
    uint mesh_id;         // offset: 76   size:  4
};

layout(std430, binding = 15) buffer QuadBuffer
{
    Quad ssbo_Quads[];
};

uvec3 _get_relative_chunk_coord(uint pos) {
    // Packing scheme is defined by LocalSectionIndex
    return (uvec3(pos) >> uvec3(5u, 0u, 2u)) & uvec3(7u, 3u, 7u);
}

vec3 _get_draw_translation(uint pos) {
    return u_RegionOffset + (vec3(_get_relative_chunk_coord(pos)) * vec3(16.0));
}

const vec2 CORNERS[4] = vec2[] (
  vec2(1.0, 1.0),
  vec2(0.0, 1.0),
  vec2(0.0, 0.0),
  vec2(1.0, 0.0)
);

out vec2 v_TexCoord;
out vec2 v_RelCoord;

#ifdef USE_FOG
out float v_FragDistance;
#endif

flat out uvec4 v_PackedColor;
flat out uvec2 v_PackedLight;

flat out uint v_Material;

void main() {
    int quad_index   = gl_VertexID >> 2;
    int corner_index = gl_VertexID  & 3;

    v_RelCoord      = CORNERS[corner_index];

    uvec3 packed_position = uvec3(
         ((ssbo_Quads[quad_index].position_lo >> uvec3(corner_index << 3)) & uvec3(0xFF)) |
        (((ssbo_Quads[quad_index].position_hi >> uvec3(corner_index << 3)) & uvec3(0xFF)) << 8)
    );

    v_PackedColor   = ssbo_Quads[quad_index].color.wzyx;

    v_TexCoord = vec2(
         ((ssbo_Quads[quad_index].tex_diffuse_lo >> uvec2(corner_index << 3)) & uvec2(0xFF)) |
        (((ssbo_Quads[quad_index].tex_diffuse_hi >> uvec2(corner_index << 3)) & uvec2(0xFF)) << 8)
    ) / 65535.0;

    v_PackedLight   = ssbo_Quads[quad_index].light;
    v_Material      = ssbo_Quads[quad_index].material;

    uint mesh_id    = ssbo_Quads[quad_index].mesh_id;

    vec3 position = (vec3(packed_position) * MODEL_SCALE) - MODEL_ORIGIN;
    position += _get_draw_translation(mesh_id);

    // Transform the vertex position into model-view-projection space
    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);

#ifdef USE_FOG
    v_FragDistance = getFragDistance(u_FogShape, position);
#endif
}
