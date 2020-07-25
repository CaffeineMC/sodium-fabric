#version 110

// Interpolated vertex attributes
varying vec4 v_Color; // The interpolated vertex color
varying vec2 v_TexCoord; // The interpolated block texture coordinates
varying vec2 v_LightCoord; // The interpolated light map texture coordinates
varying float v_MipFactor;

// Uniforms
uniform sampler2D u_BlockTex; // The block texture sampler
uniform sampler2D u_LightTex; // The light map texture sampler

#ifdef USE_MULTITEX
uniform sampler2D u_BlockTexMipped; // The block texture sampler (with mipmapping)
#endif

#ifdef USE_FOG
varying float v_FragDistance;
uniform vec4 u_FogColor; // The color of the fog
#endif

#ifdef USE_FOG_EXP2
uniform float u_FogDensity;

// e^(-density * c^2)
float getFogFactor() {
    float dist = v_FragDistance * u_FogDensity;
    return 1.0 / exp2(dist * dist);
}
#endif

#ifdef USE_FOG_LINEAR
uniform float u_FogLength; // FogStart - FogEnd
uniform float u_FogEnd;

// (end - dist) / (end - start)
float getFogFactor() {
    return (u_FogEnd - v_FragDistance) / u_FogLength;
}
#endif

void main() {
    vec4 sampleBlockTex;

#ifdef USE_MULTITEX
    sampleBlockTex = mix(
        texture2D(u_BlockTex, v_TexCoord),
        texture2D(u_BlockTexMipped, v_TexCoord),
        v_MipFactor);
#else
    sampleBlockTex = texture2D(u_BlockTex, v_TexCoord);
#endif

    // TODO: Mipmap textures have partial transparency and render as black
    // We should try to fix this outside of shader code
    // This re-implements the glAlphaFunc call that Minecraft uses for cutout rendering
    if (sampleBlockTex.a < 0.5) {
        sampleBlockTex.a = 0.0;
    }

    // Light map texture sample
    vec4 sampleLightTex = texture2D(u_LightTex, v_LightCoord);

    // Blend the colors from both textures and the vertex itself
    vec4 diffuseColor = sampleBlockTex * sampleLightTex * v_Color;

#ifdef USE_FOG
    float fogFactor = clamp(getFogFactor(), 0.0, 1.0);

    gl_FragColor = mix(u_FogColor, diffuseColor, fogFactor);
    gl_FragColor.a = diffuseColor.a;
#else
    // No fog is being used, so the fragment color is just that of the blended texture color
    gl_FragColor = diffuseColor;
#endif
}
