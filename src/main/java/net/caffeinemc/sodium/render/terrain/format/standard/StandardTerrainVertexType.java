package net.caffeinemc.sodium.render.terrain.format.standard;

import net.caffeinemc.gfx.api.array.attribute.VertexAttributeFormat;
import net.caffeinemc.gfx.api.array.attribute.VertexFormat;
import net.caffeinemc.sodium.render.terrain.format.TerrainMeshAttribute;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexSink;
import net.caffeinemc.sodium.render.terrain.format.TerrainVertexType;
import net.caffeinemc.sodium.render.vertex.buffer.VertexBufferView;
import net.caffeinemc.sodium.render.vertex.type.BlittableVertexType;
import net.minecraft.client.render.VertexConsumer;

/**
 * Simple vertex format which uses single-precision floating point numbers to represent position and texture
 * coordinates.
 */
public class StandardTerrainVertexType implements TerrainVertexType {
    public static final VertexFormat<TerrainMeshAttribute> VERTEX_FORMAT = VertexFormat.builder(TerrainMeshAttribute.class, 32)
            .addElement(TerrainMeshAttribute.POSITION, 0, VertexAttributeFormat.FLOAT, 3, false, false)
            .addElement(TerrainMeshAttribute.COLOR, 12, VertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
            .addElement(TerrainMeshAttribute.BLOCK_TEXTURE, 16, VertexAttributeFormat.FLOAT, 2, false, false)
            .addElement(TerrainMeshAttribute.LIGHT_TEXTURE, 24, VertexAttributeFormat.UNSIGNED_SHORT, 2, true, false)
            .build();

    @Override
    public TerrainVertexSink createFallbackWriter(VertexConsumer consumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TerrainVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return direct ? new StandardTerrainVertexBufferWriterUnsafe(buffer) : new StandardTerrainVertexBufferWriterNio(buffer);
    }

    @Override
    public BlittableVertexType<TerrainVertexSink> asBlittable() {
        return this;
    }

    @Override
    public VertexFormat<TerrainMeshAttribute> getCustomVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public float getVertexRange() {
        return 1.0f;
    }
}
