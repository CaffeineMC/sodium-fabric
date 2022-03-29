layout(std140, binding = 0) uniform CameraMatrices {
    // The projection matrix
    mat4 mat_proj;

    // The model-view matrix
    mat4 mat_modelview;

    // The model-view-projection matrix
    mat4 mat_modelviewproj;
};