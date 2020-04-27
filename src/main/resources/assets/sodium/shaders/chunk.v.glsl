#version 110

attribute vec3 a_Pos;
attribute vec4 a_Color;
attribute vec2 a_TexCoord;
attribute vec2 a_LightCoord;

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
uniform float u_FogLength; // FogEnd - FogStart
uniform float u_FogEnd;
#endif

uniform mat4 u_ProjectionMatrix;
uniform mat4 u_ModelViewMatrix;
uniform vec3 u_ModelOffset;

const float LIGHT_COORD_SCALE = 1.0 / 256.0;
const float LIGHT_COORD_OFFSET = 1.0 / 32.0;

void main() {
    vec4 viewSpacePos = u_ModelViewMatrix * vec4(a_Pos + u_ModelOffset, 1.0);
    gl_Position = u_ProjectionMatrix * viewSpacePos;

#ifdef USE_FOG_EXP2
    float dist = length(viewSpacePos) * u_FogDensity;
    v_FogFactor = clamp(1.0 / exp2(dist * dist), 0.0, 1.0);
#endif

#ifdef USE_FOG_LINEAR
    float dist = length(viewSpacePos);
    v_FogFactor = clamp((u_FogEnd - dist) / u_FogLength, 0.0, 1.0);
#endif

    v_Color = a_Color;
    v_TexCoord = a_TexCoord;
    v_LightCoord = (a_LightCoord * LIGHT_COORD_SCALE) + LIGHT_COORD_OFFSET;
}