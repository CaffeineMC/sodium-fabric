#import <sodium:include/fog.glsl>

// INPUTS
in vec3 a_Pos; // The position of the vertex
in vec4 a_Color; // The color of the vertex
in vec2 a_TexCoord; // The block texture coordinate of the vertex
in vec2 a_LightCoord; // The light map texture coordinate of the vertex

uniform mat4 u_ModelViewProjectionMatrix;
uniform vec3 u_ModelScale;
uniform vec2 u_TextureScale;

// The model translation for this draw call.
in vec4 d_ModelOffset;

// OUTPUTS
out vec4 v_Color;
out vec2 v_TexCoord;
out vec2 v_LightCoord;

#ifdef USE_FOG
out float v_FragDistance;
#endif