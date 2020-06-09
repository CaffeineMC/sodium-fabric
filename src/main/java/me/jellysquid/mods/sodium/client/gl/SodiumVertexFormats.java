package me.jellysquid.mods.sodium.client.gl;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadEncoder;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.HFloat;

public class SodiumVertexFormats {
    /**
     * Standard vertex format with single-precision floating point values. Matches vanilla's own vertex format.
     */
    public static final GlVertexFormat<ChunkMeshAttribute> CHUNK_MESH_VANILLA = GlVertexAttribute.builder(ChunkMeshAttribute.class, 32)
            .addElement(ChunkMeshAttribute.POSITION, 0, GlVertexAttributeFormat.FLOAT, 3, false)
            .addElement(ChunkMeshAttribute.COLOR, 12, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true)
            .addElement(ChunkMeshAttribute.TEXTURE, 16, GlVertexAttributeFormat.FLOAT, 2, false)
            .addElement(ChunkMeshAttribute.LIGHT, 24, GlVertexAttributeFormat.SHORT, 2, false)
            .build();

    public static final GlVertexFormat<ChunkMeshAttribute> CHUNK_MESH_COMPACT = GlVertexAttribute.builder(ChunkMeshAttribute.class, 20)
            .addElement(ChunkMeshAttribute.POSITION, 0, GlVertexAttributeFormat.HALF_FLOAT, 3, false)
            .addElement(ChunkMeshAttribute.COLOR, 8, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true)
            .addElement(ChunkMeshAttribute.TEXTURE, 12, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, true)
            .addElement(ChunkMeshAttribute.LIGHT, 16, GlVertexAttributeFormat.SHORT, 2, false)
            .build();

    private static final Reference2ObjectMap<GlVertexFormat<?>, ModelQuadEncoder> encoders = new Reference2ObjectOpenHashMap<>();

    static {
        registerEncoder(CHUNK_MESH_VANILLA, ModelQuadView::copyInto);
        registerEncoder(CHUNK_MESH_COMPACT, (quad, buffer, position) -> {
            for (int i = 0; i < 4; i++) {
                buffer.putShort(position, HFloat.convertFloatToHFloat(quad.getX(i)));
                buffer.putShort(position + 2, HFloat.convertFloatToHFloat(quad.getY(i)));
                buffer.putShort(position + 4, HFloat.convertFloatToHFloat(quad.getZ(i)));
                buffer.putInt(position + 8, quad.getColor(i));
                buffer.putShort(position + 12, normalizeTex(quad.getTexU(i)));
                buffer.putShort(position + 14, normalizeTex(quad.getTexV(i)));
                buffer.putInt(position + 16, quad.getLight(i));

                position += 20;
            }
        });
    }

    public static void registerEncoder(GlVertexFormat<?> format, ModelQuadEncoder encoder) {
        if (encoders.containsKey(format)) {
            throw new IllegalStateException("Encoder already registered for format: " + format);
        }

        encoders.put(format, encoder);
    }

    public static ModelQuadEncoder getEncoder(GlVertexFormat<?> format) {
        ModelQuadEncoder encoder = encoders.get(format);

        if (encoder == null) {
            throw new NullPointerException("No encoder exists for format: " + format);
        }

        return encoder;
    }

    public enum ChunkMeshAttribute {
        POSITION,
        COLOR,
        TEXTURE,
        LIGHT
    }

    private static short normalizeTex(float value) {
        return (short) (value * 65536.0f);
    }
}
