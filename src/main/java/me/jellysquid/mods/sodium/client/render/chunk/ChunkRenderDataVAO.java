package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkMeshInfo;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexArray;
import me.jellysquid.mods.sodium.client.render.gl.GlVertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL15;

import java.util.HashMap;

public class ChunkRenderDataVAO implements ChunkRenderData {
    private static final VertexFormat VERTEX_FORMAT = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL;

    private final HashMap<RenderLayer, VertexBufferWithArray> vaos = new HashMap<>();

    public ChunkRenderDataVAO() {
        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
            this.vaos.put(layer, new VertexBufferWithArray(VERTEX_FORMAT, new GlVertexBuffer(GL15.GL_ARRAY_BUFFER), new GlVertexArray()));
        }
    }

    public VertexBufferWithArray getVertexArrayForLayer(RenderLayer layer) {
        return this.vaos.get(layer);
    }

    @Override
    public void destroy() {
        for (VertexBufferWithArray buffer : this.vaos.values()) {
            buffer.delete();
        }
    }

    @Override
    public void uploadChunk(ChunkMeshInfo mesh) {
        for (ChunkMeshInfo.MeshUpload entry : mesh.getUploads()) {
            VertexBufferWithArray array = this.vaos.get(entry.layer);

            if (array == null) {
                throw new NullPointerException("No graphics state container for layer " + entry.layer);
            }

            array.upload(entry.data);
        }
    }

    @Override
    public void deleteMeshes() {

    }
}
