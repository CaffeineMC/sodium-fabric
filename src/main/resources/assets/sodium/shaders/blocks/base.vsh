#import <sodium:include/fog.glsl>

// INPUTS
in vec3 a_Pos; // The position of the vertex
in vec4 a_Color; // The color of the vertex
in vec2 a_TexCoord; // The block texture coordinate of the vertex
in vec2 a_LightCoord; // The light map texture coordinate of the vertex
in vec3 a_ChunkOffset; // The chunk offset of the vertex, used to position the chunk within a render region

uniform mat4 u_ModelViewProjectionMatrix;
uniform vec3 u_ModelScale;
uniform vec2 u_TextureScale;
uniform vec3 u_RegionTranslation;

// OUTPUTS
out vec4 v_Color;
out vec2 v_TexCoord;
out vec2 v_LightCoord;

#ifdef USE_FOG
out float v_FragDistance;
#endif

vec3 getVertexPosition(vec3 position, vec3 chunkPosition, vec3 regionPosition, vec3 modelScale) {
    return (position * modelScale) + (chunkPosition * 16.0) + regionPosition;
}