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

#ifdef DETAIL
const float StippleTexResolution = 4.0;
const float StippleCoordScale = 1.0 / StippleTexResolution;
const float StippleHalfCoordOffset = -(StippleCoordScale / 2.0);

uniform float u_DetailNearPlane;
uniform float u_DetailFarPlane;

uniform sampler2D u_StippleTex;

float _getStipple() {
    return texture(u_StippleTex, (gl_FragCoord.xy * StippleCoordScale) + StippleHalfCoordOffset).a;
}
#endif

void main() {
#ifdef DETAIL_NONMIPPED
    if (_getStipple() <= smoothstep(u_DetailNearPlane, u_DetailFarPlane, v_FragDistance)) {
        discard;
    }
#endif

    vec4 sampleBlockTex = texture(u_BlockTex, v_TexCoord);

#ifdef DETAIL_MIPPED
    sampleBlockTex.a += smoothstep(u_DetailNearPlane - (_getStipple() * 8.0), u_DetailFarPlane, v_FragDistance);

    if (sampleBlockTex.a > 1.0) {
        sampleBlockTex.a = 1.0;
    }
#endif

#ifdef ALPHA_CUTOFF
    if (sampleBlockTex.a < ALPHA_CUTOFF) {
        discard;
    }
#endif

    vec4 sampleLightTex = texture(u_LightTex, v_LightCoord);

    vec4 diffuseColor = (sampleBlockTex * sampleLightTex);
    diffuseColor *= v_Color;

    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
}
