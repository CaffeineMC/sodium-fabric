/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.jellysquid.mods.sodium.interop.fabric.mesh;

import com.google.common.base.Preconditions;
import me.jellysquid.mods.sodium.interop.fabric.material.RenderMaterialFlags;
import me.jellysquid.mods.sodium.interop.fabric.material.RenderMaterialValue;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import me.jellysquid.mods.sodium.interop.fabric.helper.GeometryHelper;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

/**
 * Holds all the array offsets and bit-wise encoders/decoders for
 * packing/unpacking quad data in an array of integers.
 * All of this is implementation-specific - that's why it isn't a "helper" class.
 */
public abstract class EncodingFormat {
    private EncodingFormat() {
    }

    static final int HEADER_BITS = 0;
    static final int HEADER_COLOR_INDEX = 1;
    static final int HEADER_TAG = 2;
    public static final int HEADER_STRIDE = 3;

    static final int VERTEX_X;
    static final int VERTEX_Y;
    static final int VERTEX_Z;
    static final int VERTEX_COLOR;
    static final int VERTEX_U;
    static final int VERTEX_V;
    static final int VERTEX_LIGHTMAP;
    static final int VERTEX_NORMAL;
    public static final int VERTEX_STRIDE;

    public static final int QUAD_STRIDE;
    public static final int QUAD_STRIDE_BYTES;
    public static final int TOTAL_STRIDE;

    static {
        final VertexFormat format = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL;
        VERTEX_X = HEADER_STRIDE + 0;
        VERTEX_Y = HEADER_STRIDE + 1;
        VERTEX_Z = HEADER_STRIDE + 2;
        VERTEX_COLOR = HEADER_STRIDE + 3;
        VERTEX_U = HEADER_STRIDE + 4;
        VERTEX_V = VERTEX_U + 1;
        VERTEX_LIGHTMAP = HEADER_STRIDE + 6;
        VERTEX_NORMAL = HEADER_STRIDE + 7;
        VERTEX_STRIDE = format.getVertexSizeInteger();
        QUAD_STRIDE = VERTEX_STRIDE * 4;
        QUAD_STRIDE_BYTES = QUAD_STRIDE * 4;
        TOTAL_STRIDE = HEADER_STRIDE + QUAD_STRIDE;

        Preconditions.checkState(VERTEX_STRIDE == QuadView.VANILLA_VERTEX_STRIDE, "Indigo vertex stride (%s) mismatched with rendering API (%s)", VERTEX_STRIDE, QuadView.VANILLA_VERTEX_STRIDE);
        Preconditions.checkState(QUAD_STRIDE == QuadView.VANILLA_QUAD_STRIDE, "Indigo quad stride (%s) mismatched with rendering API (%s)", QUAD_STRIDE, QuadView.VANILLA_QUAD_STRIDE);
    }

    /**
     * used for quick clearing of quad buffers.
     */
    static final int[] EMPTY = new int[TOTAL_STRIDE];

    private static final int DIRECTION_MASK = MathHelper.smallestEncompassingPowerOfTwo(ModelHelper.NULL_FACE_ID) - 1;
    private static final int DIRECTION_BIT_COUNT = Integer.bitCount(DIRECTION_MASK);
    private static final int CULL_SHIFT = 0;
    private static final int CULL_INVERSE_MASK = ~(DIRECTION_MASK << CULL_SHIFT);
    private static final int LIGHT_SHIFT = CULL_SHIFT + DIRECTION_BIT_COUNT;
    private static final int LIGHT_INVERSE_MASK = ~(DIRECTION_MASK << LIGHT_SHIFT);
    private static final int NORMALS_SHIFT = LIGHT_SHIFT + DIRECTION_BIT_COUNT;
    private static final int NORMALS_COUNT = 4;
    private static final int NORMALS_MASK = (1 << NORMALS_COUNT) - 1;
    private static final int NORMALS_INVERSE_MASK = ~(NORMALS_MASK << NORMALS_SHIFT);
    private static final int GEOMETRY_SHIFT = NORMALS_SHIFT + NORMALS_COUNT;
    private static final int GEOMETRY_MASK = (1 << GeometryHelper.FLAG_BIT_COUNT) - 1;
    private static final int GEOMETRY_INVERSE_MASK = ~(GEOMETRY_MASK << GEOMETRY_SHIFT);
    private static final int MATERIAL_SHIFT = GEOMETRY_SHIFT + GeometryHelper.FLAG_BIT_COUNT;
    private static final int MATERIAL_MASK = MathHelper.smallestEncompassingPowerOfTwo(RenderMaterialFlags.VALUE_COUNT) - 1;
    private static final int MATERIAL_BIT_COUNT = Integer.bitCount(MATERIAL_MASK);
    private static final int MATERIAL_INVERSE_MASK = ~(MATERIAL_MASK << MATERIAL_SHIFT);

    static {
        Preconditions.checkArgument(MATERIAL_SHIFT + MATERIAL_BIT_COUNT <= 32, "Indigo header encoding bit count (%s) exceeds integer bit length)", TOTAL_STRIDE);
    }

    static Direction cullFace(int bits) {
        return ModelHelper.faceFromIndex((bits >> CULL_SHIFT) & DIRECTION_MASK);
    }

    static int cullFace(int bits, Direction face) {
        return (bits & CULL_INVERSE_MASK) | (ModelHelper.toFaceIndex(face) << CULL_SHIFT);
    }

    static Direction lightFace(int bits) {
        return ModelHelper.faceFromIndex((bits >> LIGHT_SHIFT) & DIRECTION_MASK);
    }

    static int lightFace(int bits, Direction face) {
        return (bits & LIGHT_INVERSE_MASK) | (ModelHelper.toFaceIndex(face) << LIGHT_SHIFT);
    }

    /**
     * indicate if vertex normal has been set - bits correspond to vertex ordinals.
     */
    static int normalFlags(int bits) {
        return (bits >> NORMALS_SHIFT) & NORMALS_MASK;
    }

    static int normalFlags(int bits, int normalFlags) {
        return (bits & NORMALS_INVERSE_MASK) | ((normalFlags & NORMALS_MASK) << NORMALS_SHIFT);
    }

    static int geometryFlags(int bits) {
        return (bits >> GEOMETRY_SHIFT) & GEOMETRY_MASK;
    }

    static int geometryFlags(int bits, int geometryFlags) {
        return (bits & GEOMETRY_INVERSE_MASK) | ((geometryFlags & GEOMETRY_MASK) << GEOMETRY_SHIFT);
    }

    static RenderMaterialValue material(int bits) {
        return RenderMaterialValue.byIndex((bits >> MATERIAL_SHIFT) & MATERIAL_MASK);
    }

    static int material(int bits, RenderMaterialValue material) {
        return (bits & MATERIAL_INVERSE_MASK) | (material.index() << MATERIAL_SHIFT);
    }
}
