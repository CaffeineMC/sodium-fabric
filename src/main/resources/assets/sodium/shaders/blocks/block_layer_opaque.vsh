#version 150 core

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_vertex.glsl>
#import <sodium:include/chunk_parameters.glsl>
#import <sodium:include/chunk_matrices.glsl>

out vec4 v_Color;
out vec2 v_TexCoord;
out vec2 v_LightCoord;

#ifdef USE_FOG
out float v_FragDistance;
#endif

uniform int u_FogShape;
uniform vec3 u_RegionOffset;

void main() {
    _vert_init();

    // Transform the chunk-local vertex position into world model space
    vec3 position = u_RegionOffset + _draw_translation + _vert_position;

#ifdef USE_FOG
    v_FragDistance = getFragDistance(u_FogShape, position);
#endif

    // Transform the vertex position into model-view-projection space
    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);

    // Pass the color and texture coordinates to the fragment shader
    v_Color = _vert_color;
    v_LightCoord = _vert_tex_light_coord;
    v_TexCoord = _vert_tex_diffuse_coord;
}
