#version 150 core

#import <sodium:blocks/base.fsh>

void main() {
    vec4 sampleBlockTex = texture(u_BlockTex, v_TexCoord);

    if (sampleBlockTex.a < 0.1) {
        discard;
    }

    vec4 sampleLightTex = texture(u_LightTex, v_LightCoord);

    vec4 diffuseColor = (sampleBlockTex * sampleLightTex);
    diffuseColor *= v_Color;

    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
}
