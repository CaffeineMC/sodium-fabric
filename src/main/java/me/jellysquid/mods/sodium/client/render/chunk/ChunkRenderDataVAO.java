package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshInfo;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexArray;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL15;

public class ChunkRenderDataVAO implements ChunkRenderData {
    private static final VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL;

    private final Reference2ReferenceArrayMap<RenderLayer, VertexBufferWithArray> vaos = new Reference2ReferenceArrayMap<>();

    public VertexBufferWithArray getVertexArrayForLayer(RenderLayer layer) {
        return this.vaos.get(layer);
    }

    @Override
    public void clearData() {
        for (VertexBufferWithArray buffer : this.vaos.values()) {
            buffer.delete();
        }

        this.vaos.clear();
    }

    @Override
    public void uploadData(ChunkMeshInfo mesh) {
        for (ChunkMeshInfo.MeshUpload entry : mesh.getUploads()) {
            VertexBufferWithArray array = this.vaos.computeIfAbsent(entry.layer, this::createData);
            array.upload(entry.data);
        }
    }

    private VertexBufferWithArray createData(RenderLayer layer) {
        return new VertexBufferWithArray(VERTEX_FORMAT, new GlVertexBuffer(GL15.GL_ARRAY_BUFFER), new GlVertexArray());
    }
}
