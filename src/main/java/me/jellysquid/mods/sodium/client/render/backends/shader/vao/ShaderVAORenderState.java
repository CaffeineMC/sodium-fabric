package me.jellysquid.mods.sodium.client.render.backends.shader.vao;

import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlVertexArrayWithBuffer;
import me.jellysquid.mods.sodium.client.render.backends.shader.AbstractShaderRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkLayerInfo;

import java.util.Collection;

public class ShaderVAORenderState extends AbstractShaderRenderState<GlVertexArrayWithBuffer> {
    public ShaderVAORenderState(GlAttributeBinding[] attributes, boolean useImmutableStorage) {
        super(attributes, useImmutableStorage);
    }

    @Override
    public void clearData() {
        for (GlVertexArrayWithBuffer tess : this.data.values()) {
            tess.delete();
        }

        this.data.clear();
    }

    @Override
    public void uploadData(Collection<ChunkLayerInfo> layers) {
        this.clearData();

        for (ChunkLayerInfo info : layers) {
            this.data.put(info.getLayer(), new GlVertexArrayWithBuffer(this.createBuffer(info), this.attributes));
        }
    }
}
