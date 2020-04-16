package me.jellysquid.mods.sodium.client.render.backends.shader;

import me.jellysquid.mods.sodium.client.gl.attribute.GlAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlImmutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.tessellation.GlTessellation;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkMesh;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import org.lwjgl.opengl.GL15;

public abstract class AbstractShaderRenderState<B extends GlTessellation> implements ChunkRenderState {
    private final B[] data = this.createTessellationArrays(BlockRenderPass.count());

    protected final GlAttributeBinding[] attributes;
    protected final boolean useImmutableStorage;

    protected AbstractShaderRenderState(GlAttributeBinding[] attributes, boolean useImmutableStorage) {
        this.attributes = attributes;
        this.useImmutableStorage = useImmutableStorage;
    }

    protected abstract B[] createTessellationArrays(int count);

    public B getDataForPass(BlockRenderPass layer) {
        return this.data[layer.ordinal()];
    }

    @Override
    public void deleteData() {
        B[] data = this.data;

        for (int i = 0; i < data.length; i++) {
            GlTessellation tess = data[i];

            if (tess != null) {
                tess.delete();

                data[i] = null;
            }
        }
    }

    protected GlBuffer createBuffer(ChunkMesh info) {
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

    protected void setData(BlockRenderPass layer, B tess) {
        this.data[layer.ordinal()] = tess;
    }
}