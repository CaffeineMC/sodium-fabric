#version 150

uniform mat4 ProjMat;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in float vertexDistance;

void main() {
    vec4 color = vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
}

