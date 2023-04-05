package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.buffer.GlStorageBlock;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat3v;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformUInt;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL42C.*;
import static org.lwjgl.opengl.GL43C.*;

public class TranslucentSortingInterface {
    private final GlStorageBlock indexBuffer;
    private final GlStorageBlock vertexBuffer;
    private final GlUniformUInt indexBaseOffset;
    private final GlUniformUInt vertexBaseOffset;
    private final GlUniformUInt quadCount;
    private final GlUniformFloat3v camera;
    public TranslucentSortingInterface(ShaderBindingContext context) {
        indexBuffer = context.bindStorageBlock(0);
        vertexBuffer = context.bindStorageBlock(1);
        indexBaseOffset = context.bindUniform("baseIndexOffset", GlUniformUInt::new);
        vertexBaseOffset = context.bindUniform("baseVertexOffset", GlUniformUInt::new);
        quadCount = context.bindUniform("quadCount", GlUniformUInt::new);
        camera = context.bindUniform("camera", GlUniformFloat3v::new);
    }

    public void setupBase(RenderRegion region) {
        indexBuffer.bindBuffer(region.indexBuffers.getBufferObject());
        vertexBuffer.bindBuffer(region.vertexBuffers.getBufferObject());
    }

    private int quadCountHolder;
    private int indexOffset;
    public void setupSection(Vector3f camera, Vector3f sectionPos, ChunkGraphicsState renderStore, ModelQuadFacing face) {
        var range = renderStore.getModelPart(face);
        quadCountHolder = range.vertexCount()>>2;
        quadCount.setInt(range.vertexCount()>>2);//Measurement is in basic quad primitives
        indexOffset = (renderStore.getIndexStart()+range.indexStart())/6;
        indexBaseOffset.setInt((renderStore.getIndexStart()+range.indexStart())/6);//The offset is in basic quad index units
        vertexBaseOffset.setInt(renderStore.getVertexSegment().getOffset()+range.vertexStart());

        this.camera.set(camera.x-sectionPos.x, camera.y-sectionPos.y, camera.z-sectionPos.z);
        //this.camera.set(16,0,0);
    }

    //Does a double dispatch with offset
    public void dispatch() {
        //glFinish();
        glMemoryBarrier(GL_ELEMENT_ARRAY_BARRIER_BIT);
        glDispatchCompute((int) Math.ceil(quadCountHolder/256.0f),1,1);
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        if (quadCountHolder > 256) {
            quadCount.setInt(quadCountHolder - 128);
            indexBaseOffset.setInt(indexOffset+128);
            glDispatchCompute((int) Math.ceil((quadCountHolder-128) / 256.0f), 1, 1);
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
        }
        //glFinish();
    }
}
