#import <sodium:include/terrain_fog.glsl>

vec4 _apply_fog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogStart, float fogEnd) {
    vec4 result = mix(fogColor, fragColor, smoothstep(fogEnd, fogStart, fragDistance));
    result.a = fragColor.a;

    return result;
}