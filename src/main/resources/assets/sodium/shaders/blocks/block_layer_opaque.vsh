#version 460 core

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_vertex.glsl>
#import <sodium:include/chunk_matrices.glsl>
#import <sodium:include/chunk_parameters.glsl>

out vec4 v_Color;
out vec2 v_TexCoord;
out vec2 v_LightCoord;

out float v_FragDistance;

void main() {
    _vert_init();
    _instance_init();

    // Transform the chunk-local vertex position into world model space
    vec3 position = _instance_data.translation + _vert_position;

    // Use the maximum of the horizontal and vertical distance to get cylindrical fog
    v_FragDistance = max(length(position.xz), abs(position.y));

    // Transform the vertex position into model-view-projection space
    gl_Position = u_ModelViewProjectionMatrix * vec4(position, 1.0);

    // Pass the color and texture coordinates to the fragment shader
    v_Color = _vert_color;
    v_TexCoord = _vert_tex_diffuse_coord;
    v_LightCoord = _vert_tex_light_coord;
}
