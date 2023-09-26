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
    public static final int TEX_UV_OFFSET = 0;
    public static final int STRIDE = 8;

    public static final GlVertexFormat<ParticleMeshAttribute> VERTEX_FORMAT = GlVertexFormat.builder(ParticleMeshAttribute.class, STRIDE)
            .addElement(ParticleMeshAttribute.TEX_COORD, TEX_UV_OFFSET, GlVertexAttributeFormat.FLOAT, 2, false, false)
            .build();

    public static final VertexFormat MC_VERTEX_FORMAT = new VertexFormat(ImmutableMap.ofEntries(
            Map.entry("in_TexCoord", TEXTURE_ELEMENT)
    ));

    public static final GlVertexAttributeBinding[] ATTRIBUTE_BINDINGS = new GlVertexAttributeBinding[] {
            new GlVertexAttributeBinding(ParticleShaderBindingPoints.ATTRIBUTE_TEXTURE,
                    VERTEX_FORMAT.getAttribute(ParticleMeshAttribute.TEX_COORD)),
    };

    public static final VertexFormatDescription VERTEX_FORMAT_DESCRIPTION = VertexFormatRegistry.instance()
            .get(MC_VERTEX_FORMAT);

    public static void put(long ptr, float u, float v) {
        TextureAttribute.put(ptr + TEX_UV_OFFSET, u, v);
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
