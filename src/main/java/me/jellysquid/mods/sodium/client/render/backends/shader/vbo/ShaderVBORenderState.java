package me.jellysquid.mods.sodium.client.render.backends.shader.vbo;

import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlVertexBuffer;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkLayerInfo;

import java.util.Collection;

public class ShaderVBORenderState extends AbstractShaderRenderState<GlVertexBuffer> {
    public ShaderVBORenderState(GlAttributeBinding[] attributes, boolean useImmutableStorage) {
        super(attributes, useImmutableStorage);
    }

    @Override
    public void clearData() {
        for (GlVertexBuffer tess : this.data.values()) {
            tess.delete();
        }

        this.data.clear();
    }

    @Override
    public void uploadData(Collection<ChunkLayerInfo> layers) {
        this.clearData();

        for (ChunkLayerInfo info : layers) {
            this.data.put(info.getLayer(), new GlVertexBuffer(this.createBuffer(info), this.attributes));
        }
    }
}
