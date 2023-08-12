#version 330 core

#import <sodium:include/fog.glsl>

in vec3 v_Color; // The interpolated vertex color
in vec3 v_Light; // The interpolated vertex light

in vec2 v_TexDiffuseCoord; // The interpolated block texture coordinates

in float v_FragDistance; // The fragment's distance from the camera

in float v_MaterialMipBias;
in float v_MaterialAlphaCutoff;

uniform sampler2D u_BlockTex; // The block atlas texture

uniform vec4 u_FogColor; // The color of the shader fog
uniform float u_FogStart; // The starting position of the shader fog
uniform float u_FogEnd; // The ending position of the shader fog

out vec4 out_FragColor; // The output fragment for the color framebuffer

void main() {
    vec4 diffuseColor = texture(u_BlockTex, v_TexDiffuseCoord, v_MaterialMipBias);

#ifdef USE_FRAGMENT_DISCARD
    if (diffuseColor.a < v_MaterialAlphaCutoff) {
        discard;
    }
#endif

    // Modulate the color (used by ambient occlusion and per-vertex colouring)
    diffuseColor.rgb *= v_Color;

    // Mix the light texture sample
    diffuseColor.rgb *= v_Light;

    out_FragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
}