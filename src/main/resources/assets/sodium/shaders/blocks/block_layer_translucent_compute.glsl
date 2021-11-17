#version 420 core

#extension GL_ARB_shader_storage_buffer_object : require
#extension GL_ARB_compute_shader : require

#define DUMMY_INDEX 10000000
#define DUMMY_DISTANCE -1000000

//These constants must match the definitions in me.jellysquid.mods.sodium.client.render.chunk.shader.ComputeshaderInterface
#define LOCAL_SIZE_X 1024
#define LOCAL_BMS 0
#define LOCAL_DISPERSE 1
#define GLOBAL_FLIP 2
#define GLOBAL_DISPERSE 3

layout(local_size_x = LOCAL_SIZE_X) in;

struct DrawParameters {
// Older AMD drivers can't handle vec3 in std140 layouts correctly
// The alignment requirement is 16 bytes (4 float components) anyways, so we're not wasting extra memory with this,
// only fixing broken drivers.
    vec4 Offset;
};

//Define packed vertex data
struct Packed {
    uint a_Pos1; //ushort[2] //x,y //The position of the vertex around the model origin
    uint a_Pos2; //ushort[2] //z,w
    uint a_Color; //The color of the vertex
    uint a_TexCoord; // The block texture coordinate of the vertex
    uint a_LightCoord; // The light texture coordinate of the vertex
};

struct IndexGroup {
    uint i1;
    uint i2;
    uint i3;
};

struct ChunkMultiDrawRange {
    uint DataOffset; //Offset into the MultiDrawEntry array that this chunk starts
    uint DataCount; //How many entries in the MultiDrawEntry array this chunk covers
    uint DataIndexCount; //The count of all indicies referenced by this chunk.
};

uniform mat4 u_ModelViewMatrix;
uniform float u_ModelScale;
uniform float u_ModelOffset;
uniform int u_IndexOffsetStride = 4; //Number of bits referenced per array entry in regionIndex
uniform int u_IndexLengthStride = 3; //Number of vertices referenced per IndexGroup
uniform int u_ExecutionType;
uniform int u_SortHeight;

layout(std140, binding = 0) uniform ubo_DrawParameters {
    DrawParameters Chunks[256];
};

/*
A chunk is "big" if the number of verts in its translucent mesh is > LOCAL_SIZE_X * 2 * 3.
If a chunk is "big" multiple dispatches are required to fully sort the chunk and therefor the region.

Compute shaders have 3 levels of granularity:
Dispatch -  A call to glDispatchCompute creates a Dispatch consisting of multiple work groups.
            The number of work groups per dispatch are defined when calling the dispatch as X, Y, and Z values.

WorkGroup - For this implementation gl_WorkGroupID.y indicates the chunk within the region that each work group is working on
            while gl_WorkGroupID.x indicates the position within the chunk, and is only used for regions where
            at least one chunk is "big"

Invocation or Thread -  The smallest unit of a compute shader. There are LOCAL_SIZE_X Invocations for each WorkGroup
                        Invocations have the distinct advantage of being able to share memory between other invocations
                        within their work group and also are able to sync execution within their work group.
*/

layout(std430, binding = 1) restrict readonly buffer region_mesh_buffer {
    Packed regionMesh[];
};

layout(std430, binding = 2) coherent buffer region_index_buffer {
    uint regionIndex[];
};

layout(std430, binding = 3) restrict readonly buffer chunk_sub_count {
    ChunkMultiDrawRange chunkMultiDrawRange[];
};

layout(std430, binding = 4) restrict readonly buffer index_offset_buffer {
    int indexOffset[];
};

layout(std430, binding = 5) restrict readonly buffer index_length_buffer {
    int indexLength[];
};

layout(std430, binding = 6) restrict readonly buffer vertex_offset_buffer {
    int vertexOffset[];
};

struct IndexDistancePair {
    IndexGroup indexGroup;
    float distance;
};

//Workgroup memory.
shared IndexDistancePair local_value[LOCAL_SIZE_X * 2];

uint getIndexOffset(uint i) {
    return indexOffset[i] / u_IndexOffsetStride;
}

uint getIndexLength(uint i) {
    return indexLength[i] / u_IndexLengthStride;
}

ChunkMultiDrawRange getSubInfo() {
    return chunkMultiDrawRange[gl_WorkGroupID.y];
}

vec4 unpackPos(Packed p) {
    uint x = p.a_Pos1 & uint(0xFFFF);
    uint y = (p.a_Pos1 >> 16);
    uint z = p.a_Pos2 & uint(0xFFFF);
    uint w = (p.a_Pos2 >> 16);
    return vec4(x,y,z,w);
}

float getAverageDistance(IndexGroup indexGroup) {
    ChunkMultiDrawRange subInfo = getSubInfo();
    uint vOffset = vertexOffset[subInfo.DataOffset];

    //Nvidia drivers need these variables defined before unpackPos
    Packed rm1 = regionMesh[indexGroup.i1 + vOffset];
    Packed rm2 = regionMesh[indexGroup.i2 + vOffset];
    Packed rm3 = regionMesh[indexGroup.i3 + vOffset];
    vec4 rawPosition1 = unpackPos(rm1);
    vec4 rawPosition2 = unpackPos(rm2);
    vec4 rawPosition3 = unpackPos(rm3);

    float dist12 = length(rawPosition1 - rawPosition2);
    float dist23 = length(rawPosition2 - rawPosition3);
    float dist31 = length(rawPosition3 - rawPosition1);
    vec4 rawPosition;
    //TODO There is probably a better way to find the longest side
    if(dist12 > dist23) {
        if(dist12 > dist31) {
            rawPosition = (rawPosition1 + rawPosition2) / 2;
        } else {
            rawPosition = (rawPosition3 + rawPosition1) / 2;
        }
    } else {
        if(dist23 > dist31) {
            rawPosition = (rawPosition2 + rawPosition3) / 2;
        } else {
            rawPosition = (rawPosition3 + rawPosition1) / 2;
        }
    }

    vec3 vertexPosition = rawPosition.xyz * u_ModelScale + u_ModelOffset;
    vec3 chunkOffset = Chunks[int(rawPosition1.w)].Offset.xyz;
    vec4 pos = u_ModelViewMatrix * vec4(chunkOffset + vertexPosition, 1.0);

    return length(pos);
}

//Convert an index into the indices array from [0..IndicesInChunk] to [0..IndicesInBuffer]
uint getFullIndex(uint index) {
    ChunkMultiDrawRange subInfo = getSubInfo();
    uint i = 0;
    while(i < subInfo.DataCount) {
        uint data = subInfo.DataOffset + i;
        if(index < getIndexLength(data)) {
            return getIndexOffset(data) + index * u_IndexLengthStride;
        }
        index = index - getIndexLength(data);
        i = i + 1;
    }
    return DUMMY_INDEX;
}

IndexGroup readIndexGroup(uint fullIndex) {
    return IndexGroup(regionIndex[fullIndex + 0], regionIndex[fullIndex + 1], regionIndex[fullIndex + 2]);
}

void writeIndexGroup(uint fullIndex, IndexGroup indexGroup) {
    regionIndex[fullIndex + 0] = indexGroup.i1;
    regionIndex[fullIndex + 1] = indexGroup.i2;
    regionIndex[fullIndex + 2] = indexGroup.i3;
}

// Performs compare-and-swap over elements held in shared, workgroup-local memory
void local_compare_and_swap(uvec2 idx){
    if (local_value[idx.x].distance < local_value[idx.y].distance) {
        IndexDistancePair tmp = local_value[idx.x];
        local_value[idx.x] = local_value[idx.y];
        local_value[idx.y] = tmp;
    }
}

// Performs full-height flip (h height) over locally available indices.
void local_flip(uint h){
    uint t = gl_LocalInvocationID.x;
    barrier();

    uint half_h = h >> 1; // Note: h >> 1 is equivalent to h / 2
    ivec2 indices =
    ivec2( h * ( ( 2 * t ) / h ) ) +
    ivec2( t % half_h, h - 1 - ( t % half_h ) );

    local_compare_and_swap(indices);
}

// Performs progressively diminishing disperse operations (starting with height h)
// on locally available indices: e.g. h==8 -> 8 : 4 : 2.
// One disperse operation for every time we can half h.
void local_disperse(in uint h){
    uint t = gl_LocalInvocationID.x;
    for ( ; h > 1 ; h /= 2 ) {

        barrier();

        uint half_h = h >> 1; // Note: h >> 1 is equivalent to h / 2
        ivec2 indices =
        ivec2( h * ( ( 2 * t ) / h ) ) +
        ivec2( t % half_h, half_h + ( t % half_h ) );

        local_compare_and_swap(indices);
    }
}

// Perform binary merge sort for local elements, up to a maximum number of elements h.
void local_bms(uint h){
    for (uint hh = 2; hh <= h; hh <<= 1) {  // note:  h <<= 1 is same as h *= 2
        local_flip(hh);
        local_disperse(hh/2);
    }
}

void global_compare_and_swap(uvec2 idx){
    uint i1 = getFullIndex(idx.x);
    uint i2 = getFullIndex(idx.y);
    if(i1 != DUMMY_INDEX && i2 != DUMMY_INDEX) {
        IndexGroup ig1 = readIndexGroup(i1);
        IndexGroup ig2 = readIndexGroup(i2);
        float distance1 = getAverageDistance(ig1);
        float distance2 = getAverageDistance(ig2);

        if (distance1 < distance2) {
            writeIndexGroup(i1, ig2);
            writeIndexGroup(i2, ig1);
        }
    }
}

// Performs full-height flip (h height) in buffer
void global_flip(uint h){
    uint t = gl_GlobalInvocationID.x;

    uint half_h = h >> 1;
    uint q = uint((2 * t) / h) * h;
    uint x = q + (t % half_h);
    uint y = q + h - (t % half_h) - 1;

    global_compare_and_swap(uvec2(x,y));
}

// Performs progressively diminishing disperse operations (starting with height h)
// One disperse operation for every time we can half h.
void global_disperse(uint h){
    uint t = gl_GlobalInvocationID.x;
    uint half_h = h >> 1;
    uint q = uint((2 * t) / h) * h;
    uint x = q + (t % half_h);
    uint y = q + (t % half_h) + half_h;
    global_compare_and_swap(uvec2(x,y));
}

void local_main(uint executionType, uint height) {
    uint t = gl_LocalInvocationID.x;
    uint offset = gl_WorkGroupSize.x * 2 * gl_WorkGroupID.x;

    uint fullIndex1 = getFullIndex(offset+t*2);
    uint fullIndex2 = getFullIndex(offset+t*2+1);
    IndexGroup rig1 = readIndexGroup(fullIndex1);
    IndexGroup rig2 = readIndexGroup(fullIndex2);
    float distance1 = getAverageDistance(rig1);
    float distance2 = getAverageDistance(rig2);

    if (fullIndex1 == DUMMY_INDEX) {
        rig1 = IndexGroup(DUMMY_INDEX, DUMMY_INDEX, DUMMY_INDEX);
        distance1 = DUMMY_DISTANCE;
    }
    if (fullIndex2 == DUMMY_INDEX) {
        rig2 = IndexGroup(DUMMY_INDEX, DUMMY_INDEX, DUMMY_INDEX);
        distance2 = DUMMY_DISTANCE;
    }

    // Each local worker must save two elements to local memory, as there
    // are twice as many elments as workers.
    local_value[t*2]   = IndexDistancePair(rig1, distance1);
    local_value[t*2+1] = IndexDistancePair(rig2, distance2);

    if (executionType == LOCAL_BMS) {
        local_bms(height);
    }
    if (executionType == LOCAL_DISPERSE) {
        local_disperse(height);
    }

    barrier();
    //Write local memory back to buffer
    IndexGroup ig1 = local_value[t*2].indexGroup;
    IndexGroup ig2 = local_value[t*2+1].indexGroup;

    if (fullIndex1 != DUMMY_INDEX) {
        writeIndexGroup(fullIndex1, ig1);
    }
    if (fullIndex2 != DUMMY_INDEX) {
        writeIndexGroup(fullIndex2, ig2);
    }
}

void main(){
    uint height = gl_WorkGroupSize.x * 2;
    uint indexLength = getSubInfo().DataIndexCount / u_IndexLengthStride;
    uint computeSize = uint(pow(2, ceil(log(indexLength)/log(2))));
    uint usedWorkgroups = (computeSize / (gl_WorkGroupSize.x * 2)) + 1;

    //Exit early for unneeded work groups
    if(gl_WorkGroupID.x >= usedWorkgroups) {
        return;
    }

    if(u_ExecutionType == LOCAL_BMS || u_ExecutionType == LOCAL_DISPERSE) {
        local_main(u_ExecutionType, u_SortHeight);
    }

    if(u_ExecutionType == GLOBAL_FLIP) {
        global_flip(u_SortHeight);
    }
    if(u_ExecutionType == GLOBAL_DISPERSE) {
        global_disperse(u_SortHeight);
    }
}