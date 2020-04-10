#version 120

in vec3 a_Pos;
in vec4 a_Color;
in vec2 a_TexCoord;
in vec2 a_LightCoord;

varying vec4 v_Color;
varying vec2 v_TexCoord;
varying vec2 v_LightCoord;

uniform mat4 u_ModelView;
uniform mat4 u_Projection;

const float LOG2 = 1.442695;

void main() {
    gl_Position = u_Projection * u_ModelView * vec4(a_Pos.x, a_Pos.y, a_Pos.z, 1.0);

    v_Color = a_Color;
    v_TexCoord = a_TexCoord;
    v_LightCoord = a_LightCoord;
}