#version 450 core

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_material.glsl>

in vec2 v_TexCoord;
in vec2 v_RelCoord;

#ifdef USE_FOG
in float v_FragDistance;
#endif

flat in uvec4 v_PackedColor;
flat in uvec2 v_PackedLight;

flat in uint v_Material;

uniform sampler2D u_LightTex;
uniform sampler2D u_BlockTex;

uniform vec4 u_FogColor;
uniform float u_FogStart;
uniform float u_FogEnd;

out vec4 out_FragColor;

float _get_shade() {
    vec4 values = vec4((v_PackedColor >> uvec4(24)) & uvec4(0xFF)) / vec4(255.0);
    values = values.zwxy;

    vec2 uv = v_RelCoord;

    // Generate an SDF
    float strength = mix(
        mix(values.w, values.z, smoothstep(0.0, 1.0, uv.x)),
        mix(values.x, values.y, smoothstep(0.0, 1.0, uv.x)),
        smoothstep(0.0, 1.0, uv.y)
    );

    // Flip the values and quantize them to 0 or 1
    values = sign(1.0 - values);

    // If there are 3 or more corners with AO, flip the values
    bool triple = dot(values, vec4(1.0)) > 2.99;

    if (triple) {
        values = 1.0 - values;
    }

    vec2 p1 = vec2(0, 1);
    vec2 p2 = vec2(1, 1);
    vec2 p3 = vec2(1, 0);
    vec2 p4 = vec2(0, 0);

    float dist = 1.414; // Maximum distance between any points in a square

    if (values.x > 0.5) dist = min(dist, distance(uv, p1));
    if (values.y > 0.5) dist = min(dist, distance(uv, p2));
    if (values.z > 0.5) dist = min(dist, distance(uv, p3));
    if (values.w > 0.5) dist = min(dist, distance(uv, p4));

    if (values.x > 0.5 && values.y > 0.5) dist = min(dist, 1.0 - uv.y);
    if (values.y > 0.5 && values.z > 0.5) dist = min(dist, 1.0 - uv.x);
    if (values.z > 0.5 && values.w > 0.5) dist = min(dist, uv.y);
    if (values.w > 0.5 && values.x > 0.5) dist = min(dist, uv.x);

    if (triple) {
        dist = 1.0 - dist;
    }

    float res = clamp(dist, 0.0, 1.0);
    res = max(res, strength);

    return pow(res, 0.75);
}

vec4 _get_color_modulator() {
    vec2 coords = v_RelCoord;

    return mix(
        mix(unpackUnorm4x8(v_PackedColor[1]), unpackUnorm4x8(v_PackedColor[0]), v_RelCoord.x),
        mix(unpackUnorm4x8(v_PackedColor[2]), unpackUnorm4x8(v_PackedColor[3]), v_RelCoord.x),
        v_RelCoord.y);
}

vec2 _get_light_coord() {
    vec4 skyLightData = unpackUnorm4x8(v_PackedLight[0]);

    float skyLight = mix(
        mix(skyLightData[1], skyLightData[0], v_RelCoord.x),
        mix(skyLightData[2], skyLightData[3], v_RelCoord.x),
        v_RelCoord.y);

    vec4 blockLightData = unpackUnorm4x8(v_PackedLight[1]);

    float blockLight = mix(
        mix(blockLightData[1], blockLightData[0], v_RelCoord.x),
        mix(blockLightData[2], blockLightData[3], v_RelCoord.x),
        v_RelCoord.y);

    return clamp(vec2(skyLight, blockLight), vec2(0.5 / 16.0), vec2(15.5 / 16.0));
}

void main() {
    float mipBias = _material_mip_bias(v_Material);
    float alphaCutoff = _material_alpha_cutoff(v_Material);

    vec4 diffuseColor = texture(u_BlockTex, v_TexCoord, mipBias);

#ifdef USE_FRAGMENT_DISCARD
    if (diffuseColor.a < alphaCutoff) {
        discard;
    }
#endif

    vec4 colorModulator = _get_color_modulator();
    colorModulator.a = _get_shade();

    vec4 lightColor = texture(u_LightTex, _get_light_coord());

    vec4 finalColor = diffuseColor;
    finalColor.rgb *= colorModulator.rgb * lightColor.rgb; // vertex color
    finalColor.rgb *= colorModulator.a; // vertex shade

    out_FragColor = _linearFog(finalColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
}