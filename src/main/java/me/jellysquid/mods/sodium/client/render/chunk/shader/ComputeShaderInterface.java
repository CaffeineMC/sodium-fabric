package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformBlock;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformMatrix4f;
import me.jellysquid.mods.sodium.client.gl.util.MultiDrawBatch;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL42C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.IntBuffer;
import java.util.ArrayList;

public class ComputeShaderInterface {
    //These constants must be the same as the constants defined in shaders/blocks/block_layer_translucent_compute.glsl
    private static final int LOCAL_BMS = 0;
    private static final int LOCAL_DISPERSE = 1;
    private static final int GLOBAL_FLIP = 2;
    private static final int GLOBAL_DISPERSE = 3;
    //1024 is the minimum defined by OpenGL spec.
    //Some cards support 2048 but then we may run into workgroup memory issues
    private static final int computeWorkGroupSizeX = 1024;
    private static final int MEMORY_BARRIERS = GL42C.GL_BUFFER_UPDATE_BARRIER_BIT | GL42C.GL_UNIFORM_BARRIER_BIT;

    public static boolean isSupported(RenderDevice instance) {
        GLCapabilities capabilities = instance.getCapabilities();
        return capabilities.OpenGL43 || (capabilities.GL_ARB_compute_shader && capabilities.GL_ARB_shader_storage_buffer_object);
    }

    private final GlUniformMatrix4f uniformModelViewMatrix;
    private final GlUniformBlock uniformBlockDrawParameters;
    private final GlUniformFloat uniformModelScale;
    private final GlUniformFloat uniformModelOffset;
    private final GlUniformInt uniformExecutionType;
    private final GlUniformInt uniformSortHeight;
    private final ArrayList<Integer> pointerList = new ArrayList<>();
    private final ArrayList<Integer> subDataList = new ArrayList<>();

    public ComputeShaderInterface(ShaderBindingContext context) {
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformModelScale = context.bindUniform("u_ModelScale", GlUniformFloat::new);
        this.uniformModelOffset = context.bindUniform("u_ModelOffset", GlUniformFloat::new);
        this.uniformExecutionType = context.bindUniform("u_ExecutionType", GlUniformInt::new);
        this.uniformSortHeight = context.bindUniform("u_SortHeight", GlUniformInt::new);

        this.uniformBlockDrawParameters = context.bindUniformBlock("ubo_DrawParameters", 0);
    }

    public void setup(ChunkVertexType vertexType) {
        this.uniformModelScale.setFloat(vertexType.getPositionScale());
        this.uniformModelOffset.setFloat(vertexType.getPositionOffset());
    }

    /**
     * Executes the compute shader, using multiple calls to glDispatchCompute if
     * the data set is too large to be sorted in one call.
     */
    public boolean execute(CommandList commandList, MultiDrawBatch batch, RenderRegion.RenderRegionArenas arenas) {
        boolean isCheap = true;
        pointerList.clear();
        subDataList.clear();
        int chunkCount = 0;
        PointerBuffer pointerBuffer = batch.getPointerBuffer();
        IntBuffer countBuffer = batch.getCountBuffer();
        IntBuffer baseVertexBuffer = batch.getBaseVertexBuffer();

        int lastBaseVertexOffset = baseVertexBuffer.get(0);
        int subDataCount = 0;
        int totalSubDataCount = 0;
        int subDataIndexCount = 0;

        int pointer;
        int baseVertex;
        int count;
        int largestIndexCount = 0;
        while(countBuffer.hasRemaining()) {
            pointer = (int) (pointerBuffer.get());
            baseVertex = baseVertexBuffer.get();
            count = countBuffer.get();

            if(baseVertex != lastBaseVertexOffset) {
                lastBaseVertexOffset = baseVertex;

                subDataList.add(totalSubDataCount);
                subDataList.add(subDataCount);
                subDataList.add(subDataIndexCount);
                if(subDataIndexCount > largestIndexCount) {
                    largestIndexCount = subDataIndexCount;
                }
                chunkCount++;
                totalSubDataCount += subDataCount;
                subDataCount = 0;
                subDataIndexCount = 0;
            }
            pointerList.add(pointer); //IndexOffset
            subDataIndexCount += count;
            subDataCount++;
        }
        subDataList.add(totalSubDataCount);
        subDataList.add(subDataCount);
        subDataList.add(subDataIndexCount);
        if(subDataIndexCount > largestIndexCount) {
            largestIndexCount = subDataIndexCount;
        }
        chunkCount++;

        commandList.bindBufferBase(GlBufferTarget.SHADER_STORAGE_BUFFER, 1, arenas.vertexBuffers.getBufferObject());
        commandList.bindBufferBase(GlBufferTarget.SHADER_STORAGE_BUFFER, 2, arenas.indexBuffers.getBufferObject());

        GlMutableBuffer shaderBuffer;

        shaderBuffer = commandList.createMutableBuffer();
        commandList.bufferData(GlBufferTarget.SHADER_STORAGE_BUFFER, shaderBuffer, subDataList.stream().mapToInt(i -> i).toArray(), GlBufferUsage.DYNAMIC_DRAW);
        commandList.bindBufferBase(GlBufferTarget.SHADER_STORAGE_BUFFER, 3, shaderBuffer);

        shaderBuffer = commandList.createMutableBuffer();
        commandList.bufferData(GlBufferTarget.SHADER_STORAGE_BUFFER, shaderBuffer, pointerList.stream().mapToInt(i -> i).toArray(), GlBufferUsage.DYNAMIC_DRAW);
        commandList.bindBufferBase(GlBufferTarget.SHADER_STORAGE_BUFFER, 4, shaderBuffer);

        shaderBuffer = commandList.createMutableBuffer();
        commandList.bufferData(GlBufferTarget.SHADER_STORAGE_BUFFER, shaderBuffer, batch.getCountBuffer(), GlBufferUsage.DYNAMIC_DRAW);
        commandList.bindBufferBase(GlBufferTarget.SHADER_STORAGE_BUFFER, 5, shaderBuffer);

        shaderBuffer = commandList.createMutableBuffer();
        commandList.bufferData(GlBufferTarget.SHADER_STORAGE_BUFFER, shaderBuffer, batch.getBaseVertexBuffer(), GlBufferUsage.DYNAMIC_DRAW);
        commandList.bindBufferBase(GlBufferTarget.SHADER_STORAGE_BUFFER, 6, shaderBuffer);


        int maxHeight = (int) Math.pow(2, MathHelper.ceil(Math.log(largestIndexCount / 3)/Math.log(2)));
        int groups = (maxHeight / (computeWorkGroupSizeX * 2)) + 1;
        int height = computeWorkGroupSizeX * 2;

        //Begin by running a normal bitonic sort on all chunks.
        //For chunks whose translucent verticies are < maxComputeWorkGroupSizeX * 3 this
        //is the only work that needs to be done.
        uniformSortHeight.setInt(height);
        uniformExecutionType.setInt(LOCAL_BMS);
        GL43C.glDispatchCompute(groups, chunkCount, 1);
        GL43C.glMemoryBarrier(MEMORY_BARRIERS);

        height *= 2;

        //Keep getting height bigger until we cover all of n
        for(; height <= maxHeight; height *= 2) {
            isCheap = false;
            uniformExecutionType.set(GLOBAL_FLIP);
            uniformSortHeight.set(height);
            GL43C.glDispatchCompute(groups, chunkCount, 1);
            GL43C.glMemoryBarrier(MEMORY_BARRIERS);
            for(int halfHeight = height / 2; halfHeight > 1; halfHeight /= 2) {
                uniformSortHeight.set(halfHeight);
                if(halfHeight >= computeWorkGroupSizeX * 2)  {
                    uniformExecutionType.set(GLOBAL_DISPERSE);
                    GL43C.glDispatchCompute(groups, chunkCount, 1);
                    GL43C.glMemoryBarrier(MEMORY_BARRIERS);
                } else {
                    uniformExecutionType.setInt(LOCAL_DISPERSE);
                    GL43C.glDispatchCompute(groups, chunkCount, 1);
                    GL43C.glMemoryBarrier(MEMORY_BARRIERS);
                    break;
                }
            }
        }
        return isCheap;
    }

    public void setModelViewMatrix(Matrix4f matrix) {
        this.uniformModelViewMatrix.set(matrix);
    }

    public void setDrawUniforms(GlMutableBuffer buffer) {
        this.uniformBlockDrawParameters.bindBuffer(buffer);
    }
}
