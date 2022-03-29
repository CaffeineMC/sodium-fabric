package net.caffeinemc.sodium.render.terrain.format.compact;

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
public class CompactTerrainVertexType implements TerrainVertexType {
    public static final VertexFormat<TerrainMeshAttribute> VERTEX_FORMAT = VertexFormat.builder(TerrainMeshAttribute.class, 20)
            .addElement(TerrainMeshAttribute.POSITION, 0, VertexAttributeFormat.SHORT, 3, true, false)
            .addElement(TerrainMeshAttribute.COLOR, 8, VertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
            .addElement(TerrainMeshAttribute.BLOCK_TEXTURE, 12, VertexAttributeFormat.UNSIGNED_SHORT, 2, true, false)
            .addElement(TerrainMeshAttribute.LIGHT_TEXTURE, 16, VertexAttributeFormat.UNSIGNED_SHORT, 2, true, false)
            .build();

    private static final int POSITION_MAX_VALUE = 32768;
    private static final int TEXTURE_MAX_VALUE = 65536;

    private static final float POSITION_ORIGIN = 8.0f;
    private static final float POSITION_RANGE = 16.0f;
    private static final float POSITION_SCALE = POSITION_MAX_VALUE / POSITION_RANGE;

    @Override
    public TerrainVertexSink createFallbackWriter(VertexConsumer consumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TerrainVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return direct ? new CompactTerrainVertexBufferWriterUnsafe(buffer) : new CompactTerrainVertexBufferWriterNio(buffer);
    }

    @Override
    public BlittableVertexType<TerrainVertexSink> asBlittable() {
        return this;
    }

    @Override
    public VertexFormat<TerrainMeshAttribute> getCustomVertexFormat() {
        return VERTEX_FORMAT;
    }

    static short encodeBlockTexture(float v) {
        return (short) (v * TEXTURE_MAX_VALUE);
    }

    static short encodePosition(float v) {
        return (short) ((v - POSITION_ORIGIN) * POSITION_SCALE);
    }

    static int encodeLightMapTexCoord(int light) {
        int r = light;

        // Mask off coordinate values outside 0..255
        r &= 0x00FF_00FF;

        // Light coordinates are normalized values, so upcasting requires a shift
        // Scale the coordinates from the range of 0..255 (unsigned byte) into 0..65535 (unsigned short)
        r <<= 8;

        // Add a half-texel offset to each coordinate so we sample from the center of each texel
        r += 0x0800_0800;

        return r;
    }

    @Override
    public float getVertexRange() {
        return POSITION_RANGE;
    }
}
