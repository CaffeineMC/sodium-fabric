struct DrawParameters {
    // Older AMD drivers can't handle vec3 in std140 layouts correctly
    // The alignment requirement is 16 bytes (4 float components) anyways, so we're not wasting extra memory with this,
    // only fixing broken drivers.
    vec4 offset;
};

layout(std140) uniform ubo_DrawParameters {
    DrawParameters Chunks[256];
};