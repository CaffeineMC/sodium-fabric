#define MODEL_SCALE        32.0 / 65536.0
#define MODEL_ORIGIN       8.0

#define COLOR_SCALE        1.0 / 255.0

#define TEX_COORD_SCALE    1.0 / 32768.0

// The packed vertex data which is read from the vertex buffer
in uvec4 in_VertexData;

// The position of the vertex around the model origin
vec3 _vert_position;

// The block texture coordinate of the vertex
vec2 _vert_tex_coord;

// The color of the vertex
vec3 _vert_color;

// The light of the veretx
uvec2 _vert_light;

// The material data for the primitive
uint _vert_material;

// The index of the draw command which this vertex belongs to
uint _vert_mesh_id;

void _vert_init() {
    // Vertex Position
    uvec3 packed_position = uvec3(
        (in_VertexData[0] >>  0) & 0xFFFFu,
        (in_VertexData[0] >> 16) & 0xFFFFu,
        (in_VertexData[1] >>  0) & 0xFFFFu
    );
    _vert_position = (vec3(packed_position) * MODEL_SCALE) - MODEL_ORIGIN;

    // Vertex Material
    _vert_material = (in_VertexData[1] >> 16) & 0xFFu;

    // Vertex Mesh ID
    _vert_mesh_id  = (in_VertexData[1] >> 24) & 0xFFu;

    // Vertex Color
    uvec3 packed_color = (uvec3(in_VertexData[2]) >> uvec3(0, 8, 16)) & uvec3(0xFFu);
    _vert_color = vec3(packed_color) * COLOR_SCALE;

    // Vertex Light
    uvec2 packed_light = (uvec2(in_VertexData[2]) >> uvec2(24, 28)) & uvec2(0xFu);
    _vert_light = packed_light;

    // Vertex Texture Coords
    uvec2 packed_tex_coord = (uvec2(in_VertexData[3]) >> uvec2(0, 16)) & uvec2(0xFFFFu);
    _vert_tex_coord = vec2(packed_tex_coord) * TEX_COORD_SCALE;
}