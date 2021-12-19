#version 330 core
#extension GL_ARB_shader_storage_buffer_object : require
#extension GL_ARB_shading_language_packing : require

#moj_import <light.glsl>

// We use pull rendering - these values are in vertBufferSsbo
// See PullVert for more information
// in vec3 Position;
// in vec2 UV0;
// in vec3 Normal;
// in uint PartId;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform mat4 ProjMat;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

uniform int InstanceOffset;// minecraft doesn't have a way to set uints
uniform int InstanceVertCount;

struct ModelPart {
    mat4 modelViewMat;
    mat3x4 normalMat;
};
layout(std140, binding = 1) readonly restrict buffer modelPartsLayout {
    ModelPart[] modelParts;
} modelPartsSsbo;

struct Model {
    vec4 Color;
    ivec2 UV1;
    ivec2 UV2;
    vec3 Padding;
    uint PartOffset;
};

layout(std140, binding = 2) readonly restrict buffer modelsLayout {
    Model[] models;
} modelsSsbo;

// This is in the same format as the VBO that would normally get passed in. Since we're doing
// funny stuff with the index buffer (our instance hack requires us to reference indices multiple
// times with different values in the EBO, since we have no way of telling which model we belong
// to since gl_VertexID is the thing stored in the EBO), we have to use pull rendering.
//
// Pull rendering means that we grab the data for our vertex manually - this is designed to exactly
// mirror the normal vertex attributes that we would be passed in. Specifically we have to match
// the layout of BakedMinecraftModelsVertexFormats.SMART_ENTITY_FORMAT
struct PullVert {
    vec3 Position;
// Note there is an implicit four bytes of padding here from std140
    vec2 UV0;
    uint PackedNormal;// Format is byte-byte-byte-unused - see unpackSnorm4x8
    uint PartId;
};
layout(std140, binding = 3) readonly restrict buffer vertBufferLayout {
    PullVert[] verts;
} vertBufferSsbo;


out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec4 normal;

void main() {
    int instanceId = gl_VertexID / InstanceVertCount;
    PullVert pv = vertBufferSsbo.verts[gl_VertexID % InstanceVertCount];
    vec3 Normal = unpackSnorm4x8(pv.PackedNormal).xyz;// Drop the fourth byte, it's unused
    Model model = modelsSsbo.models[InstanceOffset + instanceId];
    ModelPart modelPart = modelPartsSsbo.modelParts[model.PartOffset + pv.PartId];

    vec4 multipliedPosition = modelPart.modelViewMat * vec4(pv.Position, 1.0);
    gl_Position = ProjMat * multipliedPosition;

    vertexDistance = length(multipliedPosition.xyz);
    vec3 multipliedNormal = normalize(mat3(modelPart.normalMat) * Normal);
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, multipliedNormal, model.Color);
    lightMapColor = texelFetch(Sampler2, model.UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, model.UV1, 0);
    texCoord0 = pv.UV0;
    normal = ProjMat * vec4(multipliedNormal, 0.0);
}
