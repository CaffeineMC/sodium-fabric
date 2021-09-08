package me.jellysquid.mods.sodium.render.chunk.format;

import me.jellysquid.mods.thingl.attribute.VertexAttributeFormat;
import me.jellysquid.mods.thingl.attribute.VertexFormat;
import me.jellysquid.mods.sodium.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.model.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.model.vertex.type.ChunkVertexType;
import net.minecraft.client.render.VertexConsumer;

/**
 * Simple vertex format which uses single-precision floating point numbers to represent position and texture
 * coordinates.
 */
public class ModelVertexType implements ChunkVertexType {
    public static final ModelVertexType INSTANCE = new ModelVertexType();

    public static final VertexFormat<ChunkMeshAttribute> VERTEX_FORMAT = VertexFormat.builder(ChunkMeshAttribute.class, 24)
            .addElement(ChunkMeshAttribute.POSITION_ID, 0, VertexAttributeFormat.UNSIGNED_SHORT, 4, false)
            .addElement(ChunkMeshAttribute.COLOR, 8, VertexAttributeFormat.UNSIGNED_BYTE, 4, true)
            .addElement(ChunkMeshAttribute.BLOCK_TEXTURE, 12, VertexAttributeFormat.UNSIGNED_SHORT, 2, false)
            .addElement(ChunkMeshAttribute.LIGHT_TEXTURE, 16, VertexAttributeFormat.UNSIGNED_SHORT, 2, true)
            .addElement(ChunkMeshAttribute.BLOCK_FLAGS, 20, VertexAttributeFormat.INT, 1, false)
            .build();


    @Override
    public ModelVertexSink createFallbackWriter(VertexConsumer consumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return direct ? new ModelVertexBufferWriterUnsafe(buffer) : new ModelVertexBufferWriterNio(buffer);
    }

    @Override
    public BlittableVertexType<ModelVertexSink> asBlittable() {
        return this;
    }

    @Override
    public VertexFormat<ChunkMeshAttribute> getCustomVertexFormat() {
        return VERTEX_FORMAT;
    }

}
