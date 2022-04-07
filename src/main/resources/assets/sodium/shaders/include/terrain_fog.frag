layout(std140, binding = 3) uniform FogParametersFS {
    // The color of the shader fog
    vec4 fog_color;

    // The starting position of the shader fog
    float fog_start;

    // The ending position of the shader fog
    float fog_end;
};

vec4 _apply_fog(vec4 fragColor, float fragDistance, vec4 fogColor, float fogStart, float fogEnd) {
    vec4 result = mix(fogColor, fragColor, smoothstep(fogEnd, fogStart, fragDistance));
    result.a = fragColor.a;

    return result;
}