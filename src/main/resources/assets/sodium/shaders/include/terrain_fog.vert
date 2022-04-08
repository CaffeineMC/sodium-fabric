const int FOG_SHAPE_SPHERICAL = 0;
const int FOG_SHAPE_CYLINDRICAL = 1;

#import <sodium:include/terrain_fog.glsl>

float _get_fog_depth(vec3 position) {
    switch (fog_mode) {
        case FOG_SHAPE_SPHERICAL: return length(position);
        case FOG_SHAPE_CYLINDRICAL: return max(length(position.xz), abs(position.y));

        // This shouldn't be possible to get, but return a sane value just in case
        default: return 0.0f;
    }
}