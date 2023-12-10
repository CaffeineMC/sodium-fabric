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
    vec4 lightColor = texture(u_LightTex, _get_light_coord());

    vec4 finalColor = diffuseColor;
    finalColor.rgb *= colorModulator.rgb * lightColor.rgb; // vertex color
    finalColor.rgb *= colorModulator.a; // vertex shade

    out_FragColor = _linearFog(finalColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
}