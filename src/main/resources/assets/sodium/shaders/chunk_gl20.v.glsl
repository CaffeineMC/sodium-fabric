#version 110
#ifdef USE_MULTIDRAW
#extension GL_ARB_shader_draw_parameters : require
#endif

attribute vec3 a_Pos; // The position of the vertex
attribute vec4 a_Color; // The color of the vertex
attribute vec2 a_TexCoord; // The block texture coordinate of the vertex
attribute vec2 a_LightCoord; // The light map texture coordinate of the vertex

varying vec4 v_Color;
varying vec2 v_TexCoord;
varying vec2 v_LightCoord;

#ifdef USE_FOG
varying float v_FogFactor;
#endif

#ifdef USE_FOG_EXP2
uniform float u_FogDensity;
#endif

#ifdef USE_FOG_LINEAR
uniform float u_FogLength; // FogStart - FogEnd
uniform float u_FogEnd;
#endif

uniform mat4 u_ModelViewProjectionMatrix;
uniform vec3 u_ModelScale;

#ifdef USE_MULTIDRAW
uniform vec4 u_ModelOffsets[MAX_BATCH_SIZE];
#else
uniform vec3 u_ModelOffset;
#endif

void main() {
    // The model translation for this draw call.
    vec3 modelOffset;

#ifdef USE_MULTIDRAW
    modelOffset = u_ModelOffsets[gl_DrawIDARB].xyz;
#else
    modelOffset = u_ModelOffset;
#endif

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
    v_LightCoord = a_LightCoord;
}