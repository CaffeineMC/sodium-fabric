package me.jellysquid.mods.sodium.client.render.chunk.format.xhfp;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.chunk.format.hfp.HFPModelVertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.render.chunk.format.hfp.HFPModelVertexBufferWriterUnsafe;

import net.minecraft.client.render.VertexConsumer;

/**
 * Like HFPModelVertexType, but extended to support Iris. The extensions aren't particularly efficient right now.
 */
public class XHFPModelVertexType implements ChunkVertexType {
    public static final GlVertexFormat<ChunkMeshAttribute> VERTEX_FORMAT = GlVertexAttribute.builder(ChunkMeshAttribute.class, 48)
            .addElement(ChunkMeshAttribute.POSITION, 0, GlVertexAttributeFormat.UNSIGNED_SHORT, 3, false)
            .addElement(ChunkMeshAttribute.COLOR, 8, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true)
            .addElement(ChunkMeshAttribute.TEXTURE, 12, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false)
            .addElement(ChunkMeshAttribute.LIGHT, 16, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, true)
            .addElement(ChunkMeshAttribute.MID_TEX_COORD, 20, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, true)
            .addElement(ChunkMeshAttribute.TANGENT, 24, GlVertexAttributeFormat.BYTE, 4, true)
            .addElement(ChunkMeshAttribute.NORMAL, 28, GlVertexAttributeFormat.BYTE, 3, true)
            // This is really dumb - we don't need 16 bytes, we need 2 at most
            .addElement(ChunkMeshAttribute.BLOCK_ID, 32, GlVertexAttributeFormat.FLOAT, 4, false)
            .build();

    public static final float MODEL_SCALE = (32.0f / 65536.0f);
    public static final float TEXTURE_SCALE = (1.0f / 32768.0f);

    @Override
    public ModelVertexSink createFallbackWriter(VertexConsumer consumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        //return direct ? new HFPModelVertexBufferWriterUnsafe(buffer) : new HFPModelVertexBufferWriterNio(buffer);

        return new XHFPModelVertexBufferWriterNio(buffer);
    }

    @Override
    public BlittableVertexType<ModelVertexSink> asBlittable() {
        return this;
    }

    @Override
    public GlVertexFormat<ChunkMeshAttribute> getCustomVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public float getModelScale() {
        return MODEL_SCALE;
    }

    @Override
    public float getTextureScale() {
        return TEXTURE_SCALE;
    }
}
