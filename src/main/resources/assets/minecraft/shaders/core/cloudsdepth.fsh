#version 150

uniform mat4 ProjMat;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in float vertexDistance;

// Custom cloud fog algorithm by Balint, for use in Sodium
void main() {
    vec4 color = vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
}

