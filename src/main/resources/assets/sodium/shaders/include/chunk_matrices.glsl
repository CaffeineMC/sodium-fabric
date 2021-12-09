// The projection matrix
uniform mat4 u_ProjectionMatrix;

// The model-view matrix
uniform mat4 u_ModelViewMatrix;

// The model-view-projection matrix
#define u_ModelViewProjectionMatrix uProjectionMatrix * u_ModelViewMatrix