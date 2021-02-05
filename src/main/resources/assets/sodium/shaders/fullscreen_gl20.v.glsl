#version 130

void main() {
    float x = (gl_VertexID & 1)*4 - 1;
    float y = (gl_VertexID & 2)*2 - 1;
    gl_Position = vec4(x, y, 0, 1);
}
