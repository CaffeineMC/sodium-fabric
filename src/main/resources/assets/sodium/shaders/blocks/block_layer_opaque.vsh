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

void main() {
    _vert_init();

    vec4 position = u_ModelViewMatrix * vec4(_draw_translation + _vert_position, 1.0);

#ifdef USE_FOG
    v_FragDistance = cylindrical_distance(position);
#endif

    // Transform the vertex position into model-view-projection space
    gl_Position = u_ProjectionMatrix * position;

    // Pass the color and texture coordinates to the fragment shader
    v_Color = _vert_color;
    v_LightCoord = _vert_tex_light_coord;
    v_TexCoord = _vert_tex_diffuse_coord;
}
