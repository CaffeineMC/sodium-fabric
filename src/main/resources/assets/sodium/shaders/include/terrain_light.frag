vec4 _sample_lightmap(sampler2D lightMap, vec2 uv) {
    return texture(lightMap, clamp(uv, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}
