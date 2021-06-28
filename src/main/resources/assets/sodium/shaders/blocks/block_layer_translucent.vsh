#version 150 core

#import <sodium:blocks/base.vsh>

void main() {
    vec3 pos = getVertexPosition();

#ifdef USE_FOG
    v_FragDistance = length(pos);
#endif

    // Transform the vertex position into model-view-projection space
    gl_Position = u_ModelViewProjectionMatrix * vec4(pos, 1.0);

    // Pass the color and texture coordinates to the fragment shader
    v_Color = a_Color;
    v_TexCoord = a_TexCoord * u_TextureScale;
    v_LightCoord = a_LightCoord;
}

