#version 110
attribute vec3 a_Pos; // The position of the vertex
attribute vec4 a_Color; // The color of the vertex
attribute vec2 a_TexCoord; // The block texture coordinate of the vertex
attribute vec2 a_LightCoord; // The light map texture coordinate of the vertex

varying vec4 v_Color;
varying vec2 v_TexCoord;
varying vec2 v_LightCoord;

#ifdef USE_FOG
varying float v_FragDistance;
#endif

uniform mat4 u_ModelViewProjectionMatrix;
uniform vec3 u_ModelScale;
uniform vec2 u_TextureScale;

// The model translation for this draw call.
// If multi-draw is enabled, then the model offset will come from an attribute buffer.
#ifdef USE_MULTIDRAW
attribute vec4 d_ModelOffset;
#else
uniform vec4 d_ModelOffset;
#endif

void main() {
    // Translates the vertex position around the position of the camera
    // This can be used to calculate the distance of the vertex from the camera without needing to
    // transform it into model-view space with a matrix, which is much slower.
    vec3 pos = (a_Pos * u_ModelScale) + d_ModelOffset.xyz;

#ifdef USE_FOG
    v_FragDistance = length(pos);
#endif

    // Transform the vertex position into model-view-projection space
    gl_Position = u_ModelViewProjectionMatrix * vec4(pos, 1.0);

    // Pass the color and texture coordinates to the fragment shader
    v_Color = a_Color;
    v_TexCoord = a_TexCoord * u_TextureScale;
    v_LightCoord = a_LightCoord;
}

