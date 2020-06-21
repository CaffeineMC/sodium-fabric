#version 110

attribute vec3 a_Pos;

uniform mat4 u_ProjectionMatrix;
uniform mat4 u_ModelViewMatrix;
uniform vec3 u_ModelOffset;

void main() {
    vec4 viewSpacePos = u_ModelViewMatrix * vec4(a_Pos + u_ModelOffset, 1.0);

    gl_Position = u_ProjectionMatrix * viewSpacePos;
}