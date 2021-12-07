#ifdef USE_FOG
vec4 _linearFog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogStart, float fogEnd) {
    vec4 result = mix(fogColor, fragColor,
    smoothstep(fogEnd, fogStart, fragDistance));
    result.a = fragColor.a;

    return result;
}

float cylindrical_distance(vec4 pos) {
    float distXZ = length(vec4(pos.x, 0.0, pos.z, pos.w));
    float distY = length(vec4(0.0, pos.y, 0.0, pos.w));
    return max(distXZ, distY);
}
#else
vec4 _linearFog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogStart, float fogEnd) {
    return fragColor;
}

float cylindrical_distance(vec4 pos) {
    return length(pos);
}
#endif