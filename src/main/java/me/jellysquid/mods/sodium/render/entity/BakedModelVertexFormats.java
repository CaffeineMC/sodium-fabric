/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package me.jellysquid.mods.sodium.render.entity;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;

public class BakedModelVertexFormats {
    public static final VertexFormat SMART_ENTITY_FORMAT = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                    .put("Position", VertexFormats.POSITION_ELEMENT)
                    // Four bytes of padding - this rounds out the vec3 to a vec4
                    // This is required because the PullVert struct in the shader uses an alignment (std140) that
                    // aligns vec2s to the 8-byte boundary, thus leaving a gap between the position and UV.
                    // TODO is the performance gain here worth the memory tradeoff of changing the struct packing
                    //  (whether directly or declaring a bunch of floats) and saving these four bytes per vert?
                    .put("PosPad", new VertexFormatElement(0, VertexFormatElement.DataType.FLOAT, VertexFormatElement.Type.PADDING, 1))
                    .put("UV0", VertexFormats.TEXTURE_0_ELEMENT)
                    .put("Normal", VertexFormats.NORMAL_ELEMENT)
                    .put("NormPad", VertexFormats.PADDING_ELEMENT)
                    .put("PartId", new VertexFormatElement(0, VertexFormatElement.DataType.UINT, VertexFormatElement.Type.UV, 1))
                    .build());
}
