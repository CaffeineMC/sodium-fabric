#import <sodium:include/fog.glsl>

// INPUTS
in vec3 a_Origin; // The model origin of the vertex
in vec3 a_Pos; // The position of the vertex around the model origin
in vec4 a_Color; // The color of the vertex
in vec2 a_TexCoord; // The block texture coordinate of the vertex
in vec2 a_LightCoord; // The light texture coordinate of the vertex

uniform mat4 u_ModelViewProjectionMatrix;

uniform float u_ModelScale;
uniform float u_ModelOffset;

uniform float u_TextureScale;

uniform vec3 u_RegionOrigin;

// OUTPUTS
out vec4 v_Color;
out vec2 v_TexCoord;
out vec2 v_LightCoord;

#ifdef USE_FOG
out float v_FragDistance;
#endif

vec3 getVertexPosition() {
    return (a_Pos * u_ModelScale + u_ModelOffset) + a_Origin + u_RegionOrigin;
}