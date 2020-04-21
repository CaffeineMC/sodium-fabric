package me.jellysquid.mods.sodium.client.gl;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadEncoder;

public class SodiumVertexFormats {
    /**
     * Standard vertex format with single-precision floating point values. Closely mirrors the vanilla vertex format.
     */
    public static final GlVertexFormat<ChunkMeshAttribute> CHUNK_MESH_SFP = GlVertexAttribute.builder(ChunkMeshAttribute.class)
            .add(ChunkMeshAttribute.POSITION, new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, 3, false, 0))
            .add(ChunkMeshAttribute.COLOR, new GlVertexAttribute(GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, 12))
            .add(ChunkMeshAttribute.TEXTURE, new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, 2, false, 16))
            .add(ChunkMeshAttribute.LIGHT, new GlVertexAttribute(GlVertexAttributeFormat.SHORT, 2, false, 24))
            .build(28);

    /**
     * Compact vertex format which makes use of normalized unsigned shorts to represent floating point values. This
     * format normalizes the position attribute of a vertex from [-8,24] to [-1,1] with no precision loss for whole
     * values.
     */
    public static final GlVertexFormat<ChunkMeshAttribute> CHUNK_MESH_HFP = GlVertexAttribute.builder(ChunkMeshAttribute.class)
            .add(ChunkMeshAttribute.POSITION, new GlVertexAttribute(GlVertexAttributeFormat.UNSIGNED_SHORT, 3, true, 0))
            .add(ChunkMeshAttribute.COLOR, new GlVertexAttribute(GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, 8))
            .add(ChunkMeshAttribute.TEXTURE, new GlVertexAttribute(GlVertexAttributeFormat.UNSIGNED_SHORT, 2, true, 12))
            .add(ChunkMeshAttribute.LIGHT, new GlVertexAttribute(GlVertexAttributeFormat.SHORT, 2, false, 16))
            .build(20);

    private static final Reference2ObjectMap<GlVertexFormat<?>, ModelQuadEncoder> encoders = new Reference2ObjectOpenHashMap<>();

    static {
        registerEncoder(CHUNK_MESH_SFP, (buffer, position, quad, x, y, z) -> {
            for (int i = 0; i < 4; i++) {
                buffer.putFloat(position, quad.getX(i) + x);
                buffer.putFloat(position + 4, quad.getY(i) + y);
                buffer.putFloat(position + 8, quad.getZ(i) + z);
                buffer.putInt(position + 12, quad.getColor(i));
                buffer.putFloat(position + 16, quad.getTexU(i));
                buffer.putFloat(position + 20, quad.getTexV(i));
                buffer.putInt(position + 24, quad.getLight(i));

                position += 28;
            }
        });

        registerEncoder(CHUNK_MESH_HFP, (buffer, position, quad, x, y, z) -> {
            for (int i = 0; i < 4; i++) {
                buffer.putShort(position, normalizeFloat(quad.getX(i) + x));
                buffer.putShort(position + 2, normalizeFloat(quad.getY(i) + y));
                buffer.putShort(position + 4, normalizeFloat(quad.getZ(i) + z));
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

    private static short normalizeFloat(float value) {
        return (short) ((((value + 8.0f) / 32.0f) * 65536.0f) - 1.0f);
    }

    private static short normalizeTex(float value) {
        return (short) ((value * 65536.0f) - 1.0f);
    }
}
