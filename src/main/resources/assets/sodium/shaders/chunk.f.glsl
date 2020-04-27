#version 110

varying vec4 v_Color;
varying vec2 v_TexCoord;
varying vec2 v_LightCoord;

#ifdef USE_FOG
varying float v_FogFactor;
uniform vec4 u_FogColor;
#endif

uniform sampler2D u_BlockTex;
uniform sampler2D u_LightTex;

void main() {
    vec4 sampleBlockTex = texture2D(u_BlockTex, v_TexCoord);
    vec4 sampleLightTex = texture2D(u_LightTex, v_LightCoord);

    vec4 texColor = v_Color * sampleBlockTex * sampleLightTex;

#ifdef USE_FOG
    gl_FragColor = mix(u_FogColor, texColor, v_FogFactor);
    gl_FragColor.w = texColor.w;
#else
    gl_FragColor = texColor;
#endif
}