#version 430 core

const uint MAX_DRAWS = 256;

struct Vertex {
    // The position of the vertex around the model origin
    vec3 position;

    // The block texture coordinate of the vertex
    vec2 tex_diffuse_coord;

    // The light texture coordinate of the vertex
    vec2 tex_light_coord;

    // The color (rgb) and shade (alpha) of the vertex
    vec4 color_and_shade;
};

// The projection matrix
uniform mat4 u_ProjectionMatrix;

// The model-view matrix
uniform mat4 u_ModelViewMatrix;

out vec4 v_ColorAndShade;
out vec2 v_TexCoord;
out vec2 v_LightCoord;

#ifdef USE_FOG
out float v_FragDistance;
#endif

uint _get_vertex_index() {
    return uint(gl_VertexID) & 0x00FFFFFFu;
}

uint _get_instance_index() {
    return (uint(uint(gl_VertexID)) & uint(0xFF000000u)) >> uint(24u);
    // reverted back to original code
    // A: return (uint(uint(gl_VertexID)) & uint(0xFF000000u)) << uint(1u) >> uint(25u);
    // B: return (uint(double(gl_VertexID)) & uint(0xFF000000u)) >> uint(24u);
    // C: return (uint(uint(gl_VertexID)+1u) & uint(0xFF000000u)) >> uint(24u);
    // D: return (uint((uint(gl_VertexID)) & uint(0xFF000000u)) | 0x00000001u ) >> uint(24u);

    // shifting by 24u seems to corrupt the instance id for some reason. doing weird things
    // prior to shifting makes amd drivers more happy but makes nvidia throw a fit.
    // all changes here & their issues show up on nvidia gpus too. A&B chunks eventually corrupt and flicker
    // C&D certian cluster of chunks wont load. im assuming D is the same aswell for nvidia but its untested.
    // its hard to tell but it seems these cluster of chunks dont load with the original code for amd either
    // so i really have no clue why nvidia is managing to match that behaviour.

    // see also: https://www.reddit.com/r/GraphicsProgramming/comments/igosn8/gl_vertexid_skips_id_8388608_223/
}

#import <sodium:include/fog.glsl>

void _emit_vertex(Vertex vertex, vec3 offset) {;
    // Transform the chunk-local vertex position into world model space
    vec3 position = offset + vertex.position;

#ifdef USE_FOG
    v_FragDistance = length(position);
#endif

    // Transform the vertex position into model-view-projection space
    gl_Position = u_ProjectionMatrix * u_ModelViewMatrix * vec4(position, 1.0);

    // Pass the color and texture coordinates to the fragment shader
    v_ColorAndShade = vertex.color_and_shade;
    v_LightCoord = vertex.tex_light_coord;
    v_TexCoord = vertex.tex_diffuse_coord;
}