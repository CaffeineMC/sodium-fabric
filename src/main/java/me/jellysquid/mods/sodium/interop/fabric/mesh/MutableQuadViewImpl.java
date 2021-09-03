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
import me.jellysquid.mods.sodium.interop.fabric.material.RenderMaterialValue;
import me.jellysquid.mods.sodium.util.geometry.Norm3b;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import me.jellysquid.mods.sodium.interop.fabric.SodiumRenderer;
import me.jellysquid.mods.sodium.interop.fabric.helper.TextureHelper;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

import static me.jellysquid.mods.sodium.interop.fabric.mesh.EncodingFormat.*;

/**
 * Almost-concrete implementation of a mutable quad. The only missing part is {@link #emit()},
 * because that depends on where/how it is used. (Mesh encoding vs. render-time transformation).
 */
public abstract class MutableQuadViewImpl extends QuadViewImpl implements QuadEmitter {
    public final void begin(int[] data, int baseIndex) {
        this.data = data;
        this.baseIndex = baseIndex;
        this.clear();
    }

    public void clear() {
        System.arraycopy(EMPTY, 0, this.data, this.baseIndex, EncodingFormat.TOTAL_STRIDE);
        this.isGeometryInvalid = true;
        this.nominalFace = null;
        this.normalFlags(0);
        this.tag(0);
        this.colorIndex(-1);
        this.cullFace(null);
        this.material(SodiumRenderer.MATERIAL_STANDARD);
    }

    @Override
    public final MutableQuadViewImpl material(RenderMaterial material) {
        if (material == null) {
            material = SodiumRenderer.MATERIAL_STANDARD;
        }

        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.material(this.data[this.baseIndex + HEADER_BITS], (RenderMaterialValue) material);
        return this;
    }

    @Override
    public final MutableQuadViewImpl cullFace(Direction face) {
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.cullFace(this.data[this.baseIndex + HEADER_BITS], face);
        this.nominalFace(face);
        return this;
    }

    @Override
    public final MutableQuadViewImpl nominalFace(Direction face) {
        this.nominalFace = face;
        return this;
    }

    @Override
    public final MutableQuadViewImpl colorIndex(int colorIndex) {
        this.data[this.baseIndex + HEADER_COLOR_INDEX] = colorIndex;
        return this;
    }

    @Override
    public final MutableQuadViewImpl tag(int tag) {
        this.data[this.baseIndex + HEADER_TAG] = tag;
        return this;
    }

    /**
     * @deprecated will be removed in 1.17 cycle - see docs in interface
     */
    @Deprecated
    @Override
    public final MutableQuadViewImpl fromVanilla(int[] quadData, int startIndex, boolean isItem) {
        System.arraycopy(quadData, startIndex, this.data, this.baseIndex + HEADER_STRIDE, QUAD_STRIDE);
        this.isGeometryInvalid = true;
        return this;
    }

    @Override
    public final MutableQuadViewImpl fromVanilla(BakedQuad quad, RenderMaterial material, Direction cullFace) {
        System.arraycopy(quad.getVertexData(), 0, this.data, this.baseIndex + HEADER_STRIDE, QUAD_STRIDE);
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.cullFace(0, cullFace);
        this.nominalFace(quad.getFace());
        this.colorIndex(quad.getColorIndex());
        this.material(material);
        this.tag(0);
        this.shade(quad.hasShade());
        this.isGeometryInvalid = true;
        return this;
    }

    @Override
    public MutableQuadViewImpl pos(int vertexIndex, float x, float y, float z) {
        final int index = this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_X;
        this.data[index] = Float.floatToRawIntBits(x);
        this.data[index + 1] = Float.floatToRawIntBits(y);
        this.data[index + 2] = Float.floatToRawIntBits(z);
        this.isGeometryInvalid = true;
        return this;
    }

    protected void normalFlags(int flags) {
        this.data[this.baseIndex + HEADER_BITS] = EncodingFormat.normalFlags(this.data[this.baseIndex + HEADER_BITS], flags);
    }

    @Override
    public MutableQuadViewImpl normal(int vertexIndex, float x, float y, float z) {
        this.normalFlags(this.normalFlags() | (1 << vertexIndex));
        this.data[this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_NORMAL] = Norm3b.pack(x, y, z);
        return this;
    }

    @Override
    public MutableQuadViewImpl lightmap(int vertexIndex, int lightmap) {
        this.data[this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_LIGHTMAP] = lightmap;
        return this;
    }

    @Override
    public MutableQuadViewImpl spriteColor(int vertexIndex, int spriteIndex, int color) {
        Preconditions.checkArgument(spriteIndex == 0, "Unsupported sprite index: %s", spriteIndex);

        this.data[this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_COLOR] = color;
        return this;
    }

    @Override
    public MutableQuadViewImpl sprite(int vertexIndex, int spriteIndex, float u, float v) {
        Preconditions.checkArgument(spriteIndex == 0, "Unsupported sprite index: %s", spriteIndex);

        final int i = this.baseIndex + vertexIndex * VERTEX_STRIDE + VERTEX_U;
        this.data[i] = Float.floatToRawIntBits(u);
        this.data[i + 1] = Float.floatToRawIntBits(v);
        return this;
    }

    @Override
    public MutableQuadViewImpl spriteBake(int spriteIndex, Sprite sprite, int bakeFlags) {
        Preconditions.checkArgument(spriteIndex == 0, "Unsupported sprite index: %s", spriteIndex);

        TextureHelper.bakeSprite(this, spriteIndex, sprite, bakeFlags);
        return this;
    }
}
