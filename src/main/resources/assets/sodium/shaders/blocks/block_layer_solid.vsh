#version 150 core

#import <sodium:blocks/base.vsh>

void main() {
    vec4 pos = u_ModelViewMatrix * vec4(getVertexPosition(), 1.0);

    #ifdef USE_FOG
    v_FragDistance = length(pos);
    #endif

    // Transform the vertex position into model-view-projection space
    gl_Position = u_ProjectionMatrix * pos;

    // Pass the color and texture coordinates to the fragment shader
    v_Color = a_Color;
    v_TexCoord = a_TexCoord * u_TextureScale;
    v_LightCoord = a_LightCoord;
}

