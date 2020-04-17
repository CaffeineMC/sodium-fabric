#version 110

attribute vec3 a_Pos;
attribute vec4 a_Color;
attribute vec2 a_TexCoord;
attribute vec2 a_LightCoord;

varying vec4 v_Color;
varying vec2 v_TexCoord;
varying vec2 v_LightCoord;

uniform mat4 u_ModelViewProjectionMatrix;
uniform vec3 u_ModelOffset;

void main() {
    gl_Position = u_ModelViewProjectionMatrix * vec4(a_Pos + u_ModelOffset, 1.0);

    v_Color = a_Color;
    v_TexCoord = a_TexCoord;
    v_LightCoord = a_LightCoord;
}