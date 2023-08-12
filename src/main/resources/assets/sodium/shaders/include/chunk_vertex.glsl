#define MODEL_SCALE        32.0 / 65535.0
#define MODEL_ORIGIN       8.0

#define COLOR_SCALE        1.0 / 255.0

#define TEX_DIFFUSE_SCALE  1.0 / 65535.0

#define TEX_LIGHT_SCALE    1.0 / 16.0
#define TEX_LIGHT_OFFSET   0.5 / 16.0

// The packed vertex data which is read from the vertex buffer
in uvec4 in_VertexData;

// The position of the vertex around the model origin
vec3 _vert_position;

// The block texture coordinate of the vertex
vec2 _vert_tex_diffuse_coord;

// The light texture coordinate of the vertex
vec2 _vert_tex_light_coord;

// The color of the vertex
vec3 _vert_color;

// The material data for the primitive
uint _vert_material;

// The index of the draw command which this vertex belongs to
uint _vert_mesh_id;

void _vert_init() {
    // Read vertex data
    uint vertexData0 = in_VertexData[0]; // u16x2 (position.x, position.y)
    uint vertexData1 = in_VertexData[1]; // u16x2 (position.z, draw_parameters.xy)
    uint vertexData2 = in_VertexData[2]; //  u8x4 (color.r, color.g, color.b, light.uv)
    uint vertexData3 = in_VertexData[3]; // u16x2 (tex.u, tex.v)

    // Unpack vertex data
    uvec4 packed_position_draw = (uvec4(vertexData0, vertexData0, vertexData1, vertexData1) >> uvec4(0, 16, 0, 16)) & uvec4(0xFFFFu);
    uvec4 packed_color_light = (uvec4(vertexData2) >> uvec4(0, 8, 16, 24)) & uvec4(0xFFu);
    uvec2 packed_tex_diffuse = (uvec2(vertexData3) >> uvec2(0, 16)) & uvec2(0xFFFFu);

    // Vertex Position
    vec3 position = vec3(packed_position_draw.xyz);
    _vert_position = (position * MODEL_SCALE) - MODEL_ORIGIN;

    // Material/Mesh ID
    uvec2 draw_parameters = (uvec2(packed_position_draw.w) >> uvec2(0, 8)) & uvec2(0xFFu);
    _vert_material = draw_parameters.x;
    _vert_mesh_id  = draw_parameters.y;

    // Vertex Color
    vec3 color = vec3(packed_color_light.rgb);
    _vert_color = color * COLOR_SCALE;

    // Vertex Light
    uvec2 light_coord = (uvec2(packed_color_light.a) >> uvec2(0, 4)) & uvec2(0xFu);
    _vert_tex_light_coord = (vec2(light_coord) * TEX_LIGHT_SCALE) + TEX_LIGHT_OFFSET;

    // Diffuse Texture Coords
    vec2 tex_diffuse = vec2(packed_tex_diffuse);
    _vert_tex_diffuse_coord = tex_diffuse * TEX_DIFFUSE_SCALE;
}