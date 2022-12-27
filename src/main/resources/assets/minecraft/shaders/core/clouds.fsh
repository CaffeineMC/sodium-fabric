#version 150

#moj_import <fog.glsl>

uniform mat4 ProjMat;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;

in vec4 vertexColor;
in float vertexDistance;

out vec4 fragColor;

// Custom cloud fog algorithm by Balint, for use in Sodium
void main() {
    vec4 color = vertexColor * ColorModulator;

    if (color.a < 0.1) {
        discard;
    }

    float width = FogEnd - FogStart;
    float newWidth = width * 4.0;
    float fade = linear_fog_fade(vertexDistance, FogStart, FogStart + newWidth);
    fragColor = vec4(color.rgb, color.a * fade);
}

