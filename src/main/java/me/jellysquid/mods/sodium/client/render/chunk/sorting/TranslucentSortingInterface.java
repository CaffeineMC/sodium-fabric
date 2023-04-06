package me.jellysquid.mods.sodium.client.render.chunk.sorting;

import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlStorageBlock;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformBlock;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat3v;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformUInt;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL42C.*;
import static org.lwjgl.opengl.GL43C.*;

public class TranslucentSortingInterface {
    private final GlUniformBlock batchBuffer;
    private final GlStorageBlock indexBuffer;
    private final GlStorageBlock vertexBuffer;
    private final GlUniformFloat3v camera;

    public TranslucentSortingInterface(ShaderBindingContext context) {
        this.batchBuffer = new GlUniformBlock(0);
        this.indexBuffer = context.bindStorageBlock(1);
        this.vertexBuffer = context.bindStorageBlock(2);
        this.camera = context.bindUniform("regionCamera", GlUniformFloat3v::new);
    }

    public void execute(Vector3f camera, RenderRegion region, int batches, GlBuffer batchBuffer) {
        this.batchBuffer.bindBuffer(batchBuffer, batches<<4);//Each batch is 16 bytes
        this.indexBuffer.bindBuffer(region.indexBuffers.getBufferObject());
        this.vertexBuffer.bindBuffer(region.vertexBuffers.getBufferObject());
        //this.camera.set(camera.x-region.getOriginX(), camera.y-region.getOriginY(), camera.z-region.getOriginZ());
        this.camera.set(region.getOriginX()-camera.x, region.getOriginY()-camera.y, region.getOriginZ()-camera.z);
        //this.camera.set(0,0,0);
        glMemoryBarrier(GL_ELEMENT_ARRAY_BARRIER_BIT);
        glDispatchCompute(batches,1,1);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    }
}
