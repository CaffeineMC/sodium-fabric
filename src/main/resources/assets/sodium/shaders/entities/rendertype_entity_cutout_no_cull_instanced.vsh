#version 330 core
#extension GL_ARB_shader_storage_buffer_object : require

#moj_import <light.glsl>

in vec3 Position;
in vec2 UV0;
in vec3 Normal;
in uint PartId;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform mat4 ProjMat;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

uniform int InstanceOffset; // minecraft doesn't have a way to set uints

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

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec4 normal;

void main() {
    #ifdef VULKAN
        Model model = modelsSsbo.models[gl_InstanceIndex];
    #else
        Model model = modelsSsbo.models[InstanceOffset + gl_InstanceID];
    #endif
    ModelPart modelPart = modelPartsSsbo.modelParts[model.PartOffset + PartId];

    vec4 multipliedPosition = modelPart.modelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * multipliedPosition;

    vertexDistance = length(multipliedPosition.xyz);
    vec3 multipliedNormal = normalize(mat3(modelPart.normalMat) * Normal);
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, multipliedNormal, model.Color);
    lightMapColor = texelFetch(Sampler2, model.UV2 / 16, 0);
    overlayColor = texelFetch(Sampler1, model.UV1, 0);
    texCoord0 = UV0;
    normal = ProjMat * vec4(multipliedNormal, 0.0);
}
