// The position of the vertex around the model origin
vec3 _vert_position;

// The block texture coordinate of the vertex
vec2 _vert_tex_diffuse_coord;

// The light texture coordinate of the vertex
ivec2 _vert_tex_light_coord;

// The color of the vertex
vec4 _vert_color;

// The index of the draw command which this vertex belongs to
uint _draw_id;

// The material bits for the primitive
uint _material_params;

#ifdef USE_VERTEX_COMPRESSION
in uvec4 a_PosId;
in vec4 a_Color;
in vec2 a_TexCoord;
in ivec2 a_LightCoord;

#if !defined(VERT_POS_SCALE)
#error "VERT_POS_SCALE not defined"
#elif !defined(VERT_POS_OFFSET)
#error "VERT_POS_OFFSET not defined"
#elif !defined(VERT_TEX_SCALE)
#error "VERT_TEX_SCALE not defined"
#endif

void _vert_init() {
    _vert_position = (vec3(a_PosId.xyz) * VERT_POS_SCALE + VERT_POS_OFFSET);
    _vert_tex_diffuse_coord = (a_TexCoord * VERT_TEX_SCALE);
    _vert_tex_light_coord = a_LightCoord;
    _vert_color = a_Color;

    _draw_id = (a_PosId.w >> 8u) & 0xFFu;
    _material_params = (a_PosId.w >> 0u) & 0xFFu;
}

#else
#error "Vertex compression must be enabled"
#endif