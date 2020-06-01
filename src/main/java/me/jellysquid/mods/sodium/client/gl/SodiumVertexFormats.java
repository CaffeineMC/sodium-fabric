package me.jellysquid.mods.sodium.client.gl;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadEncoder;
import me.jellysquid.mods.sodium.client.render.model.quad.ModelQuadView;

public class SodiumVertexFormats {
    /**
     * Standard vertex format with single-precision floating point values. Matches vanilla's own vertex format.
     */
    public static final GlVertexFormat<ChunkMeshAttribute> CHUNK_MESH_VANILLA = GlVertexAttribute.builder(ChunkMeshAttribute.class)
            .add(ChunkMeshAttribute.POSITION, new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, 3, false, 0))
            .add(ChunkMeshAttribute.COLOR, new GlVertexAttribute(GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, 12))
            .add(ChunkMeshAttribute.TEXTURE, new GlVertexAttribute(GlVertexAttributeFormat.FLOAT, 2, false, 16))
            .add(ChunkMeshAttribute.LIGHT, new GlVertexAttribute(GlVertexAttributeFormat.SHORT, 2, false, 24))
            .build(32);

    private static final Reference2ObjectMap<GlVertexFormat<?>, ModelQuadEncoder> encoders = new Reference2ObjectOpenHashMap<>();

    static {
        registerEncoder(CHUNK_MESH_VANILLA, ModelQuadView::copyInto);
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
