// The position of the vertex around the model origin
vec3 _vert_position;

// The block texture coordinate of the vertex
vec2 _vert_tex_diffuse_coord;

// The light texture coordinate of the vertex
vec2 _vert_tex_light_coord;

// The color of the vertex
vec4 _vert_color;

// The index of the draw command which this vertex belongs to
uint _draw_id;

// The material bits for the primitive
uint _material_params;

#ifdef USE_VERTEX_COMPRESSION
const float POSITION_MAX_COORD   = 1 << 20;
const float TEXTURE_MAX_COORD    = 1 << 15;
const float LIGHT_MAX_COORD      = 1 << 8;

const float VERTEX_SCALE = 32.0 / POSITION_MAX_COORD;
const float VERTEX_OFFSET = -8.0;

// The amount of inset the texture coordinates from the edges of the texture, to avoid texture bleeding
const float TEXTURE_BIAS_VALUE = (1.0 - (1.0 / 64.0)) / TEXTURE_MAX_COORD;

in vec3 a_PositionHi;       // 3x Unsigned 10-bit integer
in vec3 a_PositionLo;       // ...
in vec4 a_Color;            // 4x Unsigned 8-bit integer (normalized)
in vec2 a_TexCoord;         // 2x Signed 16-bit integer
in uvec4 a_LightAndData;    // 4x Unsigned 8-bit integer

vec3 _decode_position(vec3 hi, vec3 lo) {
    // The 2.10.10.10 vertex formats do not support being interpreted as integer data within the shader.
    // Because of this, we need to emulate the bitwise ops with floating-point arithmetic. There is probably no
    // performance penalty to doing this (other than making things uglier) since GPUs typically have the same
    // throughput for Fp32 mul/add and Int32 shl/or operations.

    vec3 interleaved = (hi * (1 << 10)) + lo; // (hi << 10) | lo
    vec3 normalized = (interleaved * VERTEX_SCALE) + VERTEX_OFFSET;

    return normalized;
}

vec2 _decode_texcoord(vec2 value) {
    // Magnitude is within range (0, 32768)
    // Sign bit encodes bias direction
    vec2 texcoord = abs(value) / TEXTURE_MAX_COORD;
    vec2 bias = sign(value) * TEXTURE_BIAS_VALUE;

    return texcoord - bias;
}

vec2 _decode_light(uvec2 value) {
    return vec2(value) / LIGHT_MAX_COORD;
}

void _vert_init() {
    _vert_position = _decode_position(a_PositionHi, a_PositionLo);
    _vert_color = a_Color;
    _vert_tex_diffuse_coord = _decode_texcoord(a_TexCoord);

    _vert_tex_light_coord = _decode_light(a_LightAndData.xy);

    _material_params = a_LightAndData[2];
    _draw_id = a_LightAndData[3];
}

#else
#error "Vertex compression must be enabled"
#endif