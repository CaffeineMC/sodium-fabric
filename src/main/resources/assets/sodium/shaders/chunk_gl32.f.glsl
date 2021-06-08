#version 150

in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
in vec2 v_LightCoord; // The interpolated light map texture coordinates

uniform sampler2D u_BlockTex; // The block texture sampler
uniform sampler2D u_LightTex; // The light map texture sampler

out vec4 fragColor;

#ifdef USE_FOG
in float v_FragDistance;
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

#ifdef USE_FOG_VANILLA

uniform float u_FogStart;
uniform float u_FogEnd;

float getFogFactor() {
    return smoothstep(u_FogEnd, u_FogStart, v_FragDistance);
}

#endif

void main() {
    // Block texture sample
    vec4 sampleBlockTex = texture(u_BlockTex, v_TexCoord);

    if (sampleBlockTex.a < 0.1) {
        discard;
    }

    // Light map texture sample
    vec4 sampleLightTex = texture(u_LightTex, v_LightCoord);

    // Blend the colors from both textures and the vertex itself
    vec4 diffuseColor = sampleBlockTex * sampleLightTex * v_Color ;

#ifdef USE_FOG
    float fogFactor = clamp(getFogFactor(), 0.0, 1.0);

    fragColor = mix(u_FogColor, diffuseColor, fogFactor);
    fragColor.a = diffuseColor.a;
#else
    // No fog is being used, so the fragment color is just that of the blended texture color
    fragColor = diffuseColor;
#endif

}
