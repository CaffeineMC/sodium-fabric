package me.jellysquid.mods.sodium.client.render.backends.shader.vao;

import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlVertexArrayWithBuffer;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMesh;

import java.util.Collection;

public class ShaderVAORenderState extends AbstractShaderRenderState<GlVertexArrayWithBuffer> {
    public ShaderVAORenderState(GlAttributeBinding[] attributes, boolean useImmutableStorage) {
        super(attributes, useImmutableStorage);
    }

    @Override
    protected GlVertexArrayWithBuffer[] createTessellationArrays(int count) {
        return new GlVertexArrayWithBuffer[count];
    }

    @Override
    public void uploadData(Collection<ChunkMesh> meshes) {
        this.deleteData();

        for (ChunkMesh mesh : meshes) {
            this.setData(mesh.getLayer(), new GlVertexArrayWithBuffer(this.createBuffer(mesh), this.attributes));
        }
    }
}
