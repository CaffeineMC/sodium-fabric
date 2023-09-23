#version 330 core

uniform sampler2D u_ParticleTex;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 out_FragColor;

void main() {
    vec4 color = texture(u_ParticleTex, texCoord0) * vertexColor;
    if (color.a < 0.1) {
        discard;
    }
    out_FragColor = color;
}
