#version 140
#extension GL_ARB_shader_draw_parameters : enable

in vec3 a_Pos; // The position of the vertex
in vec4 a_Color; // The color of the vertex
in vec2 a_TexCoord; // The block texture coordinate of the vertex
in vec2 a_LightCoord; // The light map texture coordinate of the vertex

out vec4 v_Color;
out vec2 v_TexCoord;
out vec2 v_LightCoord;

#ifdef USE_FOG
out float v_FogFactor;
#endif

#ifdef USE_FOG_EXP2
uniform float u_FogDensity;
#endif

#ifdef USE_FOG_LINEAR
uniform float u_FogLength; // FogStart - FogEnd
uniform float u_FogEnd;
#endif

uniform mat4 u_ModelViewProjectionMatrix;
uniform vec4 u_ModelOffsets[MAX_BATCH_SIZE];
uniform vec3 u_ModelScale;

const float LIGHT_COORD_SCALE = 1.0 / 256.0;
const float LIGHT_COORD_OFFSET = 1.0 / 32.0;

void main() {
    // The model translation for this draw call.
    vec3 modelOffset = u_ModelOffsets[gl_DrawIDARB].xyz;

    // Translates the vertex position around the position of the camera
    // This can be used to calculate the distance of the vertex from the camera without needing to
    // transform it into model-view space with a matrix, which is much slower.
    vec4 pos = vec4((a_Pos * u_ModelScale) + modelOffset, 1.0);

    // Apply the matrix transformations to the vertex position to place it into model-view-projection space
    gl_Position = u_ModelViewProjectionMatrix * pos;

#ifdef USE_FOG_EXP2
    // Exp2 fog as defined by the fixed-function pipeline
    // e^(-density * c^2)
    float dist = length(pos) * u_FogDensity;
    v_FogFactor = clamp(1.0 / exp2(dist * dist), 0.0, 1.0);
#endif

#ifdef USE_FOG_LINEAR
    // Linear fog as defined by the fixed-function pipeline
    // (end - dist) / (end - start)
    // The "length" of the linear fog is precomputed on the CPU as the result will be constant for all drawn vertices
    float dist = length(pos);
    v_FogFactor = clamp((u_FogEnd - dist) / u_FogLength, 0.0, 1.0);
#endif

    // Pass the color and texture coordinates to the fragment shader
    v_Color = a_Color;
    v_TexCoord = a_TexCoord;

    // Compute the texture coordinate on the light map which will be used in the fragment shader
    // This is more legacy cruft from vanilla's fixed function pipeline. Each light map texture coordinate is
    // normalized and centered on texels through the use of a texture transformation matrix. It's not clear to me
    // why this couldn't be pre-computed on the CPU, but here we are. This should compile to a fused-multiply-add, in
    // which case it will be faster than matrix math.
    v_LightCoord = (a_LightCoord * LIGHT_COORD_SCALE) + LIGHT_COORD_OFFSET;
}