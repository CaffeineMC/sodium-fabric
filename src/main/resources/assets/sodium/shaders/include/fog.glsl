#ifdef USE_FOG
vec4 _linearFog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogStart, float fogEnd) {
    if (fragDistance <= fogStart) {
        return fragColor;
    }

    float fogValue = fragDistance < fogEnd ? smoothstep(fogStart, fogEnd, fragDistance) : 1.0;
    return vec4(mix(fragColor.rgb, fogColor.rgb, fogValue * fogColor.a), fragColor.a);
}
float _cylindrical_distance(vec3 pos) {
    float distXZ = length(vec3(pos.x, 0.0, pos.z));
    float distY = length(vec3(0.0, pos.y, 0.0));
    return max(distXZ, distY);
}
#else
vec4 _linearFog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogStart, float fogEnd) {
    return fragColor;
}
float _cylindrical_distance(vec4 pos) {
    return length(pos);
}
#endif