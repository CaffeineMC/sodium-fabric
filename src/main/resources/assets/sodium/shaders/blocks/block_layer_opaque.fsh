#version 150 core
#extension GL_ARB_texture_query_lod : require

#import <sodium:include/fog.glsl>
#import <sodium:include/chunk_material.glsl>

in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
in vec2 v_LightCoord; // The interpolated light texture coordinates
in float v_FragDistance; // The fragment's distance from the camera

flat in uint v_Material;

uniform sampler2D u_BlockTex; // The block texture sampler
uniform sampler2D u_LightTex; // The light map texture sampler

uniform vec4 u_FogColor; // The color of the shader fog
uniform float u_FogStart; // The starting position of the shader fog
uniform float u_FogEnd; // The ending position of the shader fog

out vec4 fragColor; // The output fragment for the color framebuffer

void main() {
    float mipLevel;

    if (_material_is_mipped(v_Material)) {
        mipLevel = textureQueryLOD(u_BlockTex, v_TexCoord).x;
    } else {
        mipLevel = 0.0f;
    }

    vec4 diffuseColor = textureLod(u_BlockTex, v_TexCoord, mipLevel);
    vec4 lightColor = texture(u_LightTex, v_LightCoord);

#ifdef USE_FRAGMENT_DISCARD
    float alphaCutoff = _material_alpha_cutoff(v_Material);

    if (diffuseColor.a < alphaCutoff) {
        discard;
    }
#endif

    // Apply per-vertex color
    diffuseColor.rgb *= v_Color.rgb;

    // Apply ambient occlusion "shade"
    diffuseColor.rgb *= v_Color.a;

    // Apply light map to diffuse color
    diffuseColor.rgb *= lightColor.rgb;

    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
}