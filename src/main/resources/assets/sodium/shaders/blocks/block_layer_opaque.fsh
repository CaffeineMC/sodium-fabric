#version 150 core

#import <sodium:include/fog.glsl>

in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
in vec2 v_LightCoord; // The interpolated light map texture coordinates
in float v_FragDistance; // The fragment's distance from the camera

uniform sampler2D u_BlockTex; // The block texture sampler
uniform sampler2D u_LightTex; // The light map texture sampler

uniform vec4 u_FogColor; // The color of the shader fog
uniform float u_FogStart; // The starting position of the shader fog
uniform float u_FogEnd; // The ending position of the shader fog

out vec4 fragColor; // The output fragment for the color framebuffer

vec4 _sample_lightmap(sampler2D lightMap, vec2 uv) {
    return texture(lightMap, clamp(uv, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}

void main() {
    vec4 sampleBlockTex = texture(u_BlockTex, v_TexCoord);

#ifdef ALPHA_CUTOFF
    if (sampleBlockTex.a < ALPHA_CUTOFF) {
        discard;
    }
#endif

    vec4 sampleLightTex = _sample_lightmap(u_LightTex, v_LightCoord);

    vec4 diffuseColor = (sampleBlockTex * sampleLightTex);
    diffuseColor *= v_Color;

    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
}