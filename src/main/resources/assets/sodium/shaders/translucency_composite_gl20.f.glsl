#version 130

uniform sampler2D u_AccumTex;
uniform sampler2D u_RevealTex;

void main() {
    float reveal = 1.0 - texelFetch(u_RevealTex, ivec2(gl_FragCoord.xy), 0).a;
    if (reveal == 0.0) discard; // completely transparent, ignore this fragment
    vec4 accum = texelFetch(u_AccumTex, ivec2(gl_FragCoord.xy), 0);
    gl_FragColor = vec4(accum.rgb / max(accum.a, 1e-5), reveal);
}
