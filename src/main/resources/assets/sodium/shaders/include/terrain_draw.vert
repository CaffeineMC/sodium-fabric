#extension GL_ARB_shader_draw_parameters : require

const uint MAX_BATCH_SIZE = 8 * 4 * 8;

struct ModelTransform {
    // Translation of the model in world-space
    vec3 translation;
};

layout(std140, binding = 1) uniform ModelTransforms {
    ModelTransform transforms[MAX_BATCH_SIZE];
};

vec3 _apply_view_transform(vec3 position) {
    ModelTransform transform = transforms[gl_BaseInstanceARB];
    return transform.translation + position;
}