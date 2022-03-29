float _get_fog_depth(vec3 position) {
    return max(length(position.xz), abs(position.y));
}