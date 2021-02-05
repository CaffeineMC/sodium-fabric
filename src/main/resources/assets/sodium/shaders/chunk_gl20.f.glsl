#version 130

varying vec4 v_Color; // The interpolated vertex color
varying vec2 v_TexCoord; // The interpolated block texture coordinates
varying vec2 v_LightCoord; // The interpolated light map texture coordinates

uniform sampler2D u_BlockTex; // The block texture sampler
uniform sampler2D u_LightTex; // The light map texture sampler

#ifdef USE_TRANSLUCENCY
uniform sampler2D u_DepthTex; // The opaque depth buffer sampler
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
    // Block texture sample
    vec4 sampleBlockTex = texture2D(u_BlockTex, v_TexCoord);

    // Light map texture sample
    vec4 sampleLightTex = texture2D(u_LightTex, v_LightCoord);

    // Blend the colors from both textures and the vertex itself
    vec4 diffuseColor = sampleBlockTex * sampleLightTex * v_Color;

#ifdef USE_FOG
    // Fog is used, so the fragment color needs to be mixed with the fog
    // FIXME: this may not be the correct way to do fog for translucent blocks
    float fogFactor = clamp(getFogFactor(), 0.0, 1.0);
    diffuseColor.rgb = mix(u_FogColor.rgb, diffuseColor.rgb, fogFactor);
#endif

#ifdef USE_TRANSLUCENCY
    // We do depth testing in the shader because we're testing against the opaque depth buffer
    if (gl_FragCoord.z > texelFetch(u_DepthTex, ivec2(gl_FragCoord.xy), 0).r) discard;

    diffuseColor.rgb *= diffuseColor.a; // Premultiply alpha
    float a = min(1.0, diffuseColor.a)*8.0 + 0.01;
    float b = 1.0 - 0.95*gl_FragCoord.z;
    float w = clamp(a*a*a * 1e8 * b*b*b, 1e-2, 3e2);
    gl_FragData[0] = diffuseColor * w;
    gl_FragData[1] = vec4(diffuseColor.a);
#else
    gl_FragData[0] = diffuseColor;
#endif
}
