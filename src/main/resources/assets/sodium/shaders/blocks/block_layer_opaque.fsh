#version 450 core

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_material.glsl>

in vec2 v_TexCoord;
in vec2 v_RelCoord;

#ifdef USE_FOG
in float v_FragDistance;
#endif

in vec3 v_ColorModulator;

flat in uint  v_PackedShade;
flat in uvec2 v_PackedLight;

flat in uint v_Material;

uniform sampler2D u_LightTex;
uniform sampler2D u_BlockTex;

#ifdef USE_FOG
uniform vec4 u_FogColor;
uniform float u_FogStart;
uniform float u_FogEnd;
#endif

out vec4 out_FragColor;

float _get_shade() {
    vec4 values = unpackUnorm4x8(v_PackedShade);
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

    float dist = 1.414; // Maximum distance between any points in a square

    if (values.x > 0.5) dist = min(dist, distance(uv, vec2(0, 1)));
    if (values.y > 0.5) dist = min(dist, distance(uv, vec2(1, 1)));
    if (values.z > 0.5) dist = min(dist, distance(uv, vec2(1, 0)));
    if (values.w > 0.5) dist = min(dist, distance(uv, vec2(0, 0)));

    if (values.x > 0.5 && values.y > 0.5) dist = min(dist, 1.0 - uv.y);
    if (values.y > 0.5 && values.z > 0.5) dist = min(dist, 1.0 - uv.x);
    if (values.z > 0.5 && values.w > 0.5) dist = min(dist, uv.y);
    if (values.w > 0.5 && values.x > 0.5) dist = min(dist, uv.x);

    if (triple) {
        dist = 1.0 - dist;
    }

    float res = clamp(dist, strength, 1.0);

    return pow(res, 0.75);
}

vec3 _get_light() {
    vec4 light01 = unpackUnorm4x8(v_PackedLight[0]); // (c0.x, c0.y, c1.x, c1.y)
    vec4 light23 = unpackUnorm4x8(v_PackedLight[1]); // (c3.x, c3.y, c2.x, c2.y)

    vec2 uv = mix(
        mix(light01.xy, light01.zw, v_RelCoord.x),
        mix(light23.zw, light23.xy, v_RelCoord.x),
        v_RelCoord.y);

    uv = clamp(uv, vec2(0.5 / 16.0), vec2(15.5 / 16.0));

    return texture(u_LightTex, uv).rgb;
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

    vec4 finalColor = diffuseColor;
    finalColor.rgb *= v_ColorModulator;
    finalColor.rgb *= _get_light();
    finalColor.rgb *= _get_shade();

#ifdef USE_FOG
    finalColor = _linearFog(finalColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
#endif

    out_FragColor = finalColor;
}