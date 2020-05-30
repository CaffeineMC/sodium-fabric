#version 110

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

uniform mat4 u_ProjectionMatrix;
uniform mat4 u_ModelViewMatrix;
uniform vec3 u_ModelOffset;

const float LIGHT_COORD_SCALE = 1.0 / 256.0;
const float LIGHT_COORD_OFFSET = 1.0 / 32.0;

void main() {
    // Translate the vertex's position by the model offset of the chunk being rendered
    vec4 vPos = vec4(a_Pos + u_ModelOffset, 1.0);

    // Calculate the position of the vertex in modelview-space for use in fog distance calculations
    // The vertex's position will already be translated by the chunk's offset above, but for compatibility reasons we
    // still use the modelview matrix on the fixed function pipeline's stack. This shouldn't impact performance at all
    // as the model and view matrices have been pre-multiplied and we would've had to multiply by the view matrix
    // anyways.
    vec4 viewSpacePos = u_ModelViewMatrix * vPos;

    // Calculate the position of the vertex in projected space and pass it along for rendering
    gl_Position = u_ProjectionMatrix * viewSpacePos;

#ifdef USE_FOG_EXP2
    // Exp2 fog as defined by the fixed-function pipeline
    // e^(-density * c^2)
    float dist = length(viewSpacePos) * u_FogDensity;
    v_FogFactor = clamp(1.0 / exp2(dist * dist), 0.0, 1.0);
#endif

#ifdef USE_FOG_LINEAR
    // Linear fog as defined by the fixed-function pipeline
    // (end - dist) / (end - start)
    // The "length" of the linear fog is precomputed on the CPU as the result will be constant for all drawn vertices
    float dist = length(viewSpacePos);
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