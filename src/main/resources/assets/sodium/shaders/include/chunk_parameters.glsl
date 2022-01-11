const uint MAX_INSTANCES = 8 * 4 * 8;

struct InstanceData {
    vec3 translation;
};

layout(std140) uniform ubo_InstanceData {
    InstanceData instances[MAX_INSTANCES];
};

InstanceData _instance_data;

void _instance_init() {
    _instance_data = instances[gl_BaseInstance];
}