struct ModelTransform {
    // Translation of the model in world-space.
    // This is a vec4 because there are bugs with alignment in some drivers with vec3.
    // Instead, we can just swizzle the first 3 components.
    vec4 translation;
};

layout(std140, binding = 1) readonly restrict buffer ModelTransforms {
    ModelTransform transforms[];
};

vec3 _apply_view_transform(vec3 position) {
    ModelTransform transform = transforms[gl_BaseInstance];
    return transform.translation.xyz + position;
}