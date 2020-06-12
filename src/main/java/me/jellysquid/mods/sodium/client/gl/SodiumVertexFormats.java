package me.jellysquid.mods.sodium.client.gl;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttribute;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadEncoder;
import me.jellysquid.mods.sodium.client.util.HFloat;

public class SodiumVertexFormats {
    /**
     * Simple vertex format which uses single-precision floating point numbers to represent position and texture
     * coordinates.
     */
    public static final GlVertexFormat<ChunkMeshAttribute> CHUNK_MESH_FULL = GlVertexAttribute.builder(ChunkMeshAttribute.class, 32)
            .addElement(ChunkMeshAttribute.POSITION, 0, GlVertexAttributeFormat.FLOAT, 3, false)
            .addElement(ChunkMeshAttribute.COLOR, 12, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true)
            .addElement(ChunkMeshAttribute.TEXTURE, 16, GlVertexAttributeFormat.FLOAT, 2, false)
            .addElement(ChunkMeshAttribute.LIGHT, 24, GlVertexAttributeFormat.UNSIGNED_BYTE, 2, true)
            .build();

    /**
     * Uses half-precision floating point numbers to represent position coordinates and normalized unsigned shorts for
     * texture coordinates. All texel positions in the block diffuse texture atlas can be exactly mapped (including
     * their centering offset), as the
     */
    public static final GlVertexFormat<ChunkMeshAttribute> CHUNK_MESH_COMPACT = GlVertexAttribute.builder(ChunkMeshAttribute.class, 20)
            .addElement(ChunkMeshAttribute.POSITION, 0, GlVertexAttributeFormat.HALF_FLOAT, 3, false)
            .addElement(ChunkMeshAttribute.COLOR, 8, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true)
            .addElement(ChunkMeshAttribute.TEXTURE, 12, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, true)
            .addElement(ChunkMeshAttribute.LIGHT, 16, GlVertexAttributeFormat.UNSIGNED_BYTE, 2, true)
            .build();

    private static final Reference2ObjectMap<GlVertexFormat<?>, ModelQuadEncoder> encoders = new Reference2ObjectOpenHashMap<>();

    static {
        registerEncoder(CHUNK_MESH_FULL, (quad, buffer, position) -> {
            for (int i = 0; i < 4; i++) {
                buffer.putFloat(position, quad.getX(i));
                buffer.putFloat(position + 4, quad.getY(i));
                buffer.putFloat(position + 8, quad.getZ(i));
                buffer.putInt(position + 12, quad.getColor(i));
                buffer.putFloat(position + 16, quad.getTexU(i));
                buffer.putFloat(position + 20, quad.getTexV(i));
                buffer.putInt(position + 24, transformLightTexCoord(quad.getLight(i)));

                position += 32;
            }
        });

        registerEncoder(CHUNK_MESH_COMPACT, (quad, buffer, position) -> {
            for (int i = 0; i < 4; i++) {
                buffer.putShort(position, HFloat.convertFloatToHFloat(quad.getX(i)));
                buffer.putShort(position + 2, HFloat.convertFloatToHFloat(quad.getY(i)));
                buffer.putShort(position + 4, HFloat.convertFloatToHFloat(quad.getZ(i)));
                buffer.putInt(position + 8, quad.getColor(i));
                buffer.putShort(position + 12, denormalizeBlockTexCoord(quad.getTexU(i)));
                buffer.putShort(position + 14, denormalizeBlockTexCoord(quad.getTexV(i)));
                buffer.putInt(position + 16, transformLightTexCoord(quad.getLight(i)));

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

    /**
     * Transforms light map texture coordinates into normalized coordinate space and adds a half-pixel offset to
     * center them on a given texel. In vanilla, this is (rather unnecessarily) performed by applying a matrix
     * transformation to all light map texture coordinates, wasting cycles.
     * @param light A vanilla texture coordinate into the light map (two shorts packed into an int)
     * @return A texture coordinate for consumption
     */
    private static int transformLightTexCoord(int light) {
        int sl = (light >> 16) & 0xFF;
        int bl = light & 0xFF;

        return ((sl + 8) << 8) | (bl + 8);
    }

    /**
     * De-normalizes a texture coordinate into a unsigned short value on the block texture atlas. This shouldn't
     * introduce any accuracy errors as the de-normalization constant is a power-of-two and the texture size limit is
     * significantly smaller than 32768x32768. The Java type returned by this method is a signed short, so one must be
     * careful when working with it as an unsigned value.
     * @param value The normalized floating point coordinate
     * @return A de-normalized coordinate in the range 0..65536
     */
    private static short denormalizeBlockTexCoord(float value) {
        return (short) (value * 65536.0f);
    }
}
