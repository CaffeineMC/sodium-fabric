#version 140

in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
in vec2 v_LightCoord; // The interpolated light map texture coordinates

#ifdef USE_FOG
in float v_FogFactor; // Normalized blend ratio for fog color
uniform vec4 u_FogColor; // The color of the fog
#endif

uniform sampler2D u_BlockTex; // The block texture sampler
uniform sampler2D u_LightTex; // The light map texture sampler

// gl_FragColor is deprecated
out vec4 outColor;

void main() {
    // Block texture sample
    vec4 sampleBlockTex = texture2D(u_BlockTex, v_TexCoord);

    // Light map texture sample
    vec4 sampleLightTex = texture2D(u_LightTex, v_LightCoord);

    // Blend the colors from both textures and the vertex itself
    vec4 diffuseColor = sampleBlockTex * sampleLightTex * v_Color;

#ifdef USE_FOG
    // Mix the fog color into the texture color by the fog factor
    outColor = vec4(mix(u_FogColor.rgb, diffuseColor.rgb, v_FogFactor), diffuseColor.a);
#else
    // No fog is being used, so the fragment color is just that of the blended texture color
    outColor = diffuseColor;
#endif
}