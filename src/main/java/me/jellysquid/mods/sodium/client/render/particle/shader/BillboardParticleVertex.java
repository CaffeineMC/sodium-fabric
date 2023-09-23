package me.jellysquid.mods.sodium.client.render.particle.shader;

import com.google.common.collect.ImmutableMap;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.LightAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.PositionAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatRegistry;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.system.MemoryUtil;

import java.util.Map;

import static net.minecraft.client.render.VertexFormats.*;

public class BillboardParticleVertex {
    public static final int POSITION_OFFSET = 0;
    public static final int SIZE_OFFSET = 12;
    public static final int TEX_UV_OFFSET = 16;
    public static final int COLOR_OFFSET = 24;
    public static final int LIGHT_UV_OFFSET = 28;
    public static final int ANGLE_OFFSET = 32;
    public static final int STRIDE = 36;

    public static final GlVertexFormat<ParticleMeshAttribute> VERTEX_FORMAT = GlVertexFormat.builder(ParticleMeshAttribute.class, STRIDE)
            .addElement(ParticleMeshAttribute.POSITION, POSITION_OFFSET, GlVertexAttributeFormat.FLOAT, 3, false, false)
            .addElement(ParticleMeshAttribute.SIZE, SIZE_OFFSET, GlVertexAttributeFormat.FLOAT, 1, false, false)
            .addElement(ParticleMeshAttribute.TEX_COORD, TEX_UV_OFFSET, GlVertexAttributeFormat.FLOAT, 2, false, false)
            .addElement(ParticleMeshAttribute.COLOR, COLOR_OFFSET, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
            .addElement(ParticleMeshAttribute.LIGHT_UV, LIGHT_UV_OFFSET, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, true)
            .addElement(ParticleMeshAttribute.ANGLE, ANGLE_OFFSET, GlVertexAttributeFormat.FLOAT, 1, false, false)
            .build();

    public static final VertexFormat MC_VERTEX_FORMAT = new VertexFormat(ImmutableMap.ofEntries(
            Map.entry("in_Position", POSITION_ELEMENT),
            Map.entry("in_Size", new VertexFormatElement(
                    0,
                    VertexFormatElement.ComponentType.FLOAT,
                    VertexFormatElement.Type.GENERIC,
                    1
            )),
            Map.entry("in_TexCoord", TEXTURE_ELEMENT),
            Map.entry("in_Color", COLOR_ELEMENT),
            Map.entry("in_Light", LIGHT_ELEMENT),
            Map.entry("in_Angle", new VertexFormatElement(
                    0,
                    VertexFormatElement.ComponentType.FLOAT,
                    VertexFormatElement.Type.GENERIC,
                    1
            ))
    ));

    public static final GlVertexAttributeBinding[] ATTRIBUTE_BINDINGS = new GlVertexAttributeBinding[] {
            new GlVertexAttributeBinding(ParticleShaderBindingPoints.ATTRIBUTE_POSITION,
                    VERTEX_FORMAT.getAttribute(ParticleMeshAttribute.POSITION)),
            new GlVertexAttributeBinding(ParticleShaderBindingPoints.ATTRIBUTE_SIZE,
                    VERTEX_FORMAT.getAttribute(ParticleMeshAttribute.SIZE)),
            new GlVertexAttributeBinding(ParticleShaderBindingPoints.ATTRIBUTE_TEXTURE,
                    VERTEX_FORMAT.getAttribute(ParticleMeshAttribute.TEX_COORD)),
            new GlVertexAttributeBinding(ParticleShaderBindingPoints.ATTRIBUTE_COLOR,
                    VERTEX_FORMAT.getAttribute(ParticleMeshAttribute.COLOR)),
            new GlVertexAttributeBinding(ParticleShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                    VERTEX_FORMAT.getAttribute(ParticleMeshAttribute.LIGHT_UV)),
            new GlVertexAttributeBinding(ParticleShaderBindingPoints.ATTRIBUTE_ANGLE,
                    VERTEX_FORMAT.getAttribute(ParticleMeshAttribute.ANGLE)),
    };

    public static final VertexFormatDescription VERTEX_FORMAT_DESCRIPTION = VertexFormatRegistry.instance()
            .get(MC_VERTEX_FORMAT);

    public static void put(long ptr, float x, float y, float z,
                           float u, float v, int color, int light, float size, float angle) {
        PositionAttribute.put(ptr + POSITION_OFFSET, x, y, z);
        MemoryUtil.memPutFloat(ptr + SIZE_OFFSET, size);
        TextureAttribute.put(ptr + TEX_UV_OFFSET, u, v);
        ColorAttribute.set(ptr + COLOR_OFFSET, color);
        LightAttribute.set(ptr + LIGHT_UV_OFFSET, light);
        MemoryUtil.memPutFloat(ptr + ANGLE_OFFSET, angle);
    }

    public static void bindVertexFormat() {
        for (GlVertexAttributeBinding attrib : ATTRIBUTE_BINDINGS) {
            if (attrib.isIntType()) {
                GL30C.glVertexAttribIPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat(),
                        attrib.getStride(), attrib.getPointer());
            } else {
                GL20C.glVertexAttribPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat(), attrib.isNormalized(),
                        attrib.getStride(), attrib.getPointer());
            }
            GL20C.glEnableVertexAttribArray(attrib.getIndex());
        }
    }
}
