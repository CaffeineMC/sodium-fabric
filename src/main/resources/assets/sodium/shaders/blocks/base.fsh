#import <sodium:include/fog.glsl>

// INPUTS
in vec4 v_Color; // The interpolated vertex color
in vec2 v_TexCoord; // The interpolated block texture coordinates
in vec2 v_LightCoord; // The interpolated light map texture coordinates

uniform sampler2D u_BlockTex; // The block texture sampler
uniform sampler2D u_LightTex; // The light map texture sampler

in float v_FragDistance; // The fragment's distance from the camera

uniform vec4 u_FogColor; // The color of the shader fog
uniform float u_FogStart; // The starting position of the shader fog
uniform float u_FogEnd; // The ending position of the shader fog

// OUTPUTS
out vec4 fragColor; // The output fragment for the color framebuffer