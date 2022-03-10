#version 460 core

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_vertex.glsl>

const uint MAX_INSTANCES = 8 * 4 * 8;

struct InstanceData {
    vec3 translation;
};

layout(std140, binding = 0) uniform ubo_CameraMatrices {
    // The projection matrix
    mat4 u_ProjectionMatrix;

    // The model-view matrix
    mat4 u_ModelViewMatrix;

    // The model-view-projection matrix
    mat4 u_ModelViewProjectionMatrix;
};

layout(std140, binding = 1) uniform ubo_InstanceData {
    InstanceData instances[MAX_INSTANCES];
};

out vec4 v_Color;
out vec2 v_TexCoord;
out vec2 v_LightCoord;

out float v_FragDistance;

void main() {
    _vert_init();

    // Transform the chunk-local vertex position into world model space
    InstanceData instance = instances[gl_BaseInstance];
    vec3 position = instance.translation + _vert_position;

    // Use the maximum of the horizontal and vertical distance to get cylindrical fog
    v_FragDistance = max(length(position.xz), abs(position.y));

    // Transform the vertex position into model-view-projection space
    gl_Position = u_ModelViewProjectionMatrix * vec4(position, 1.0);

    // Pass the color and texture coordinates to the fragment shader
    v_Color = _vert_color;
    v_TexCoord = _vert_tex_diffuse_coord;
    v_LightCoord = _vert_tex_light_coord;
}
