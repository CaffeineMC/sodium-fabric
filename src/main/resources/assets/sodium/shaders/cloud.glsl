struct VertexOutput {
    vec4 color;
    float distance;
};

layout(std140, binding = 0) uniform CameraMatrices {
    mat4 mat_proj;
    mat4 mat_modelview;

    vec4 color_mod;

    float fog_start;
    float fog_end;
};