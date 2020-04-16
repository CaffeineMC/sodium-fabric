package me.jellysquid.mods.sodium.client.render.backends.shader.vbo;

import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlVertexBuffer;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMesh;

import java.util.Collection;

public class ShaderVBORenderState extends AbstractShaderRenderState<GlVertexBuffer> {
    public ShaderVBORenderState(GlAttributeBinding[] attributes, boolean useImmutableStorage) {
        super(attributes, useImmutableStorage);
    }

    @Override
    protected GlVertexBuffer[] createTessellationArrays(int count) {
        return new GlVertexBuffer[count];
    }

    @Override
    public void uploadData(Collection<ChunkMesh> meshes) {
        this.deleteData();

        for (ChunkMesh mesh : meshes) {
            this.setData(mesh.getLayer(), new GlVertexBuffer(this.createBuffer(mesh), this.attributes));
        }
    }
}
