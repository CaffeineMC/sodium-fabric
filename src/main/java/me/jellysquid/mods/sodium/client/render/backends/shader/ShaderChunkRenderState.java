package me.jellysquid.mods.sodium.client.render.backends.shader;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArray;
import me.jellysquid.mods.sodium.client.gl.array.GlVertexArrayWithBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlImmutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.buffer.GlTessellation;
import me.jellysquid.mods.sodium.client.render.backends.ChunkRenderState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkLayerInfo;
import me.jellysquid.mods.sodium.client.render.layer.BlockRenderPass;
import org.lwjgl.opengl.GL15;

import java.util.Collection;

public class ShaderChunkRenderState implements ChunkRenderState {
    private final Reference2ReferenceArrayMap<BlockRenderPass, GlTessellation> meshes = new Reference2ReferenceArrayMap<>();

    private final boolean useVertexArrays;
    private final boolean useImmutableStorage;

    public ShaderChunkRenderState(boolean useVertexArrays, boolean useImmutableStorage) {
        this.useVertexArrays = useVertexArrays;
        this.useImmutableStorage = useImmutableStorage;
    }

    public GlTessellation getVertexArrayForLayer(BlockRenderPass layer) {
        return this.meshes.get(layer);
    }

    @Override
    public void clearData() {
        for (GlTessellation tess : this.meshes.values()) {
            tess.delete();
        }

        this.meshes.clear();
    }

    @Override
    public void uploadData(Collection<ChunkLayerInfo> layers) {
        for (GlTessellation tess : this.meshes.values()) {
            tess.delete();
        }

        this.meshes.clear();

        for (ChunkLayerInfo info : layers) {
            this.meshes.put(info.getLayer(), this.createTessellation(info));
        }
    }

    private GlTessellation createTessellation(ChunkLayerInfo info) {
        if (this.useVertexArrays) {
            return this.createVertexArrayTessellation(info);
        } else {
            return this.createSimpleTessellation(info);
        }
    }

    private GlTessellation createVertexArrayTessellation(ChunkLayerInfo info) {
        GlVertexArray array = new GlVertexArray();
        array.bind();

        GlBuffer buffer = this.createBuffer();
        buffer.bind();
        buffer.upload(info.takePendingUpload());
        buffer.bindVertexAttributes(0L);
        buffer.unbind();

        array.unbind();

        return new GlVertexArrayWithBuffer(buffer, array);
    }

    private GlTessellation createSimpleTessellation(ChunkLayerInfo info) {
        GlVertexArray array = new GlVertexArray();
        array.bind();

        GlBuffer buffer = this.createBuffer();
        buffer.bind();
        buffer.upload(info.takePendingUpload());
        buffer.unbind();

        array.unbind();

        return buffer;
    }

    private GlBuffer createBuffer() {
        GlBuffer buffer;

        if (this.useImmutableStorage) {
            buffer = new GlImmutableBuffer(GL15.GL_ARRAY_BUFFER);
        } else {
            buffer = new GlMutableBuffer(GL15.GL_ARRAY_BUFFER);
        }

        return buffer;
    }
}
