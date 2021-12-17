#version 430 core

#import <sodium:include/fog.glsl>

in vec4 v_ColorAndShade; // The interpolated vertex color and shade components
in vec2 v_TexCoord; // The interpolated block texture coordinates
in vec2 v_LightCoord; // The interpolated light map texture coordinates
in float v_FragDistance; // The fragment's distance from the camera

uniform sampler2D u_BlockTex; // The block texture sampler
uniform sampler2D u_LightTex; // The light map texture sampler

uniform vec4 u_FogColor; // The color of the shader fog
uniform float u_FogStart; // The starting position of the shader fog
uniform float u_FogEnd; // The ending position of the shader fog

out vec4 fragColor; // The output fragment for the color framebuffer

void _emit_frag() {
    vec4 sampleBlockTex = texture(u_BlockTex, v_TexCoord);

#ifdef ALPHA_CUTOFF
    if (sampleBlockTex.a < ALPHA_CUTOFF) {
        discard;
    }
#endif

    vec4 sampleLightTex = texture(u_LightTex, v_LightCoord);

    vec4 diffuseColor = (sampleBlockTex * sampleLightTex);
    diffuseColor.rgb *= v_ColorAndShade.rgb * v_ColorAndShade.a;

    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
}
