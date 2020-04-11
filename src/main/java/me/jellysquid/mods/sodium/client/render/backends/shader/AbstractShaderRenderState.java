package me.jellysquid.mods.sodium.client.render.backends.shader;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlImmutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkLayerInfo;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import org.lwjgl.opengl.GL15;

public abstract class AbstractShaderRenderState<B> implements ChunkRenderState {
    protected final Reference2ReferenceArrayMap<BlockRenderPass, B> data = new Reference2ReferenceArrayMap<>();
    protected final GlAttributeBinding[] attributes;
    protected final boolean useImmutableStorage;

    protected AbstractShaderRenderState(GlAttributeBinding[] attributes, boolean useImmutableStorage) {
        this.attributes = attributes;
        this.useImmutableStorage = useImmutableStorage;
    }

    public B getDataForPass(BlockRenderPass layer) {
        return this.data.get(layer);
    }

    protected GlBuffer createBuffer(ChunkLayerInfo info) {
        GlBuffer buffer;

        if (this.useImmutableStorage) {
            buffer = new GlImmutableBuffer(GL15.GL_ARRAY_BUFFER);
        } else {
            buffer = new GlMutableBuffer(GL15.GL_ARRAY_BUFFER);
        }

        buffer.bind();
        buffer.upload(info.takePendingUpload());
        buffer.unbind();

        return buffer;
    }
}