#ifdef USE_VERTEX_COMPRESSION
in vec4 a_PosId;
in vec4 a_Color;
in vec2 a_TexCoord;
in vec2 a_LightCoord;

#if !defined(VERT_POS_SCALE)
#error "VERT_POS_SCALE not defined"
#elif !defined(VERT_POS_OFFSET)
#error "VERT_POS_OFFSET not defined"
#elif !defined(VERT_TEX_SCALE)
#error "VERT_TEX_SCALE not defined"
#endif

// The position of the vertex around the model origin
#define _vert_position (a_PosId.xyz * VERT_POS_SCALE + VERT_POS_OFFSET)
// The block texture coordinate of the vertex
#define _vert_tex_diffuse_coord (a_TexCoord * VERT_TEX_SCALE)
// The light texture coordinate of the vertex
#define _vert_tex_light_coord a_LightCoord;
// The color of the vertex
#define _vert_color a_Color
// The index of the draw command which this vertex belongs to
#define _draw_id uint(a_PosId.w)

#else
#error "Vertex compression must be enabled"
#endif

// The translation vector of the current draw command
#define _draw_translation Chunks[_draw_id].offset.xyz