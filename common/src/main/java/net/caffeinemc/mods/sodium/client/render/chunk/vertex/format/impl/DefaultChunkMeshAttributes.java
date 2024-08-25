package net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl;

import net.caffeinemc.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import net.caffeinemc.mods.sodium.client.render.vertex.VertexFormatAttribute;

public class DefaultChunkMeshAttributes {
    public static final VertexFormatAttribute POSITION_HI = new VertexFormatAttribute("POSITION_HI", GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true);
    public static final VertexFormatAttribute POSITION_LO = new VertexFormatAttribute("POSITION_LO", GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true);
    public static final VertexFormatAttribute COLOR = new VertexFormatAttribute("COLOR", GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false);
    public static final VertexFormatAttribute TEXTURE = new VertexFormatAttribute("TEXTURE", GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, true);
    public static final VertexFormatAttribute LIGHT_MATERIAL_INDEX = new VertexFormatAttribute("LIGHT_MATERIAL_INDEX", GlVertexAttributeFormat.UNSIGNED_BYTE, 4, false, true);
}
