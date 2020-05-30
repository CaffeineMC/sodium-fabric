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
    public static final GlVertexFormat<ChunkMeshAttribute> CHUNK_MESH_VANILLA = GlVertexAttribute.builder(ChunkMeshAttribute.class)
            .add(ChunkMeshAttribute.POSITION, new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, 3, false, 0))
            .add(ChunkMeshAttribute.COLOR, new GlVertexAttribute(GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, 12))
            .add(ChunkMeshAttribute.TEXTURE, new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, 2, false, 16))
            .add(ChunkMeshAttribute.LIGHT, new GlVertexAttribute(GlVertexAttributeFormat.SHORT, 2, false, 24))
            .build(28);

    private static final Reference2ObjectMap<GlVertexFormat<?>, ModelQuadEncoder> encoders = new Reference2ObjectOpenHashMap<>();

    static {
        registerEncoder(CHUNK_MESH_VANILLA, (quad, buffer, position) -> {
            for (int i = 0; i < 4; i++) {
                buffer.putFloat(position, quad.getX(i));
                buffer.putFloat(position + 4, quad.getY(i));
                buffer.putFloat(position + 8, quad.getZ(i));
                buffer.putInt(position + 12, quad.getColor(i));
                buffer.putFloat(position + 16, quad.getTexU(i));
                buffer.putFloat(position + 20, quad.getTexV(i));
                buffer.putInt(position + 24, quad.getLight(i));

                position += 28;
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
}
