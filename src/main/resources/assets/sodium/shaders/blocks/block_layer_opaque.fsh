#version 150 core

#import <sodium:include/fog.glsl>
#import <sodium:include/block_material.glsl>

in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
in vec2 v_LightCoord; // The interpolated light map texture coordinates
in float v_FragDistance; // The fragment's distance from the camera
flat in int v_Options;

uniform sampler2D u_BlockTex; // The block texture sampler
uniform sampler2D u_LightTex; // The light map texture sampler

uniform vec4 u_FogColor; // The color of the shader fog
uniform float u_FogStart; // The starting position of the shader fog
uniform float u_FogEnd; // The ending position of the shader fog

out vec4 fragColor; // The output fragment for the color framebuffer

#ifdef DETAIL
const float StippleTexResolution = 4.0;
const float StippleCoordScale = 1.0 / StippleTexResolution;
const float StippleHalfCoordOffset = -(StippleCoordScale / 2.0);

uniform float u_DetailNearPlane;
uniform float u_DetailFarPlane;

uniform sampler2D u_StippleTex;

float getStippleValue() {
    return texture(u_StippleTex, (gl_FragCoord.xy * StippleCoordScale) + StippleHalfCoordOffset).a;
}
#endif

void main() {
    // Configures whether mipmapping will be used
    bool cutout = (v_Options & (1 << _MAT_CUTOUT)) != 0;

    // A low LOD bias is used with cutout rendering to disable mipmapping
    vec4 diffuseColor = texture(u_BlockTex, v_TexCoord, cutout ? -4.0 : 0.0);

#ifdef DETAIL
    float farPlaneOffset = getStippleValue() * -8.0;

    float detailRatio = smoothstep(u_DetailNearPlane, u_DetailFarPlane + farPlaneOffset, v_FragDistance);
    float detailDirection = (cutout ? -1.0 : 1.0);

    diffuseColor.a = clamp((detailRatio * detailDirection) + diffuseColor.a, 0.0, 1.0);
#endif

    if (diffuseColor.a < _mat_cutoutThreshold(v_Options)) discard;

    vec4 lightColor = texture(u_LightTex, v_LightCoord);

    vec4 finalColor = (diffuseColor * lightColor);
    finalColor *= v_Color;

    fragColor = _linearFog(finalColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);
}
