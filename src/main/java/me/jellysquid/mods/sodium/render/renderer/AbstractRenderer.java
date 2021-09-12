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

package me.jellysquid.mods.sodium.render.renderer;

import me.jellysquid.mods.sodium.model.light.QuadLighter;
import me.jellysquid.mods.sodium.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.interop.fabric.material.RenderMaterialValue;
import me.jellysquid.mods.sodium.model.quad.blender.BiomeBlender;
import me.jellysquid.mods.sodium.util.color.ColorARGB;
import me.jellysquid.mods.sodium.util.DirectionUtil;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;
import me.jellysquid.mods.sodium.interop.fabric.SodiumRenderer;
import me.jellysquid.mods.sodium.interop.fabric.helper.ColorHelper;
import me.jellysquid.mods.sodium.interop.fabric.helper.GeometryHelper;
import me.jellysquid.mods.sodium.interop.fabric.mesh.EncodingFormat;
import me.jellysquid.mods.sodium.interop.fabric.mesh.MeshImpl;
import me.jellysquid.mods.sodium.interop.fabric.mesh.MutableQuadViewImpl;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static me.jellysquid.mods.sodium.interop.fabric.helper.GeometryHelper.LIGHT_FACE_FLAG;

public abstract class AbstractRenderer<T extends BlockRenderInfo> {
    private static final int FULL_BRIGHTNESS = 0x00F000F0;

    protected static final int DEFAULT_TEXTURE_INDEX = 0;

    protected final T blockInfo;
    protected final QuadLighter lighter;
    protected final QuadTransform transform;

    private final int[] cachedColorOutputs = new int[4];

    private final BiomeBlender biomeBlender = BiomeBlender.create(MinecraftClient.getInstance());

    AbstractRenderer(T blockInfo, QuadLighter lighter, QuadTransform transform) {
        this.blockInfo = blockInfo;
        this.lighter = lighter;
        this.transform = transform;
    }

    private void applyQuadColorization(MutableQuadViewImpl quad, int colorIndex) {
        if (colorIndex == -1) {
            return;
        }

        this.biomeBlender.getColors(this.blockInfo.blockView, this.blockInfo.blockState, this.blockInfo.blockPos, quad,
                this.blockInfo.getColorProvider(), this.cachedColorOutputs);

        for (int i = 0; i < 4; i++) {
            quad.spriteColor(i, DEFAULT_TEXTURE_INDEX, ColorARGB.mulRGBA(this.cachedColorOutputs[i], quad.spriteColor(i, DEFAULT_TEXTURE_INDEX)));
        }
    }

    protected abstract void emitQuad(MutableQuadViewImpl quad, BlendMode renderLayer);
    
    protected void tessellateSmooth(MutableQuadViewImpl quad, BlendMode blendMode, int colorIndex) {
        this.applyQuadColorization(quad, colorIndex);

        QuadLightData light = this.lighter.getQuadLightData();

        for (int i = 0; i < 4; i++) {
            quad.spriteColor(i, DEFAULT_TEXTURE_INDEX, ColorARGB.mulRGB(quad.spriteColor(i, DEFAULT_TEXTURE_INDEX), light.shade[i]));
            quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), light.texture[i]));
        }

        this.emitQuad(quad, blendMode);
    }

    protected void tessellateSmoothEmissive(MutableQuadViewImpl quad, BlendMode blendMode, int colorIndex) {
        this.applyQuadColorization(quad, colorIndex);

        QuadLightData data = this.lighter.getQuadLightData();

        for (int i = 0; i < 4; i++) {
            quad.spriteColor(i, DEFAULT_TEXTURE_INDEX, ColorARGB.mulRGB(quad.spriteColor(i, DEFAULT_TEXTURE_INDEX), data.shade[i]));
            quad.lightmap(i, FULL_BRIGHTNESS);
        }

        this.emitQuad(quad, blendMode);
    }

    protected void tessellateFlat(MutableQuadViewImpl quad, BlendMode renderLayer, int colorIndex) {
        this.applyQuadColorization(quad, colorIndex);
        this.applyFlatShading(quad);

        final int brightness = this.applyFlatBrightness(quad, this.blockInfo.blockState, this.blockInfo.blockPos);

        for (int i = 0; i < 4; i++) {
            quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), brightness));
        }

        this.emitQuad(quad, renderLayer);
    }

    protected void tessellateFlatEmissive(MutableQuadViewImpl quad, BlendMode renderLayer, int blockColorIndex) {
        this.applyQuadColorization(quad, blockColorIndex);
        this.applyFlatShading(quad);

        for (int i = 0; i < 4; i++) {
            quad.lightmap(i, FULL_BRIGHTNESS);
        }

        this.emitQuad(quad, renderLayer);
    }

    private final BlockPos.Mutable mpos = new BlockPos.Mutable();

    private int applyFlatBrightness(MutableQuadViewImpl quad, BlockState blockState, BlockPos pos) {
        this.mpos.set(pos);

        // To mirror Vanilla's behavior, if the face has a cull-face, always sample the light value
        // offset in that direction. See net.minecraft.client.render.block.BlockModelRenderer.renderQuadsFlat
        // for reference.
        if (quad.cullFace() != null) {
            this.mpos.move(quad.cullFace());
        } else if ((quad.geometryFlags() & LIGHT_FACE_FLAG) != 0 || Block.isShapeFullCube(blockState.getCollisionShape(this.blockInfo.blockView, pos))) {
            this.mpos.move(quad.lightFace());
        }

        // Unfortunately cannot use brightness cache here unless we implement one specifically for flat lighting. See #329
        return WorldRenderer.getLightmapCoordinates(this.blockInfo.blockView, blockState, this.mpos);
    }

    private void applyFlatShading(MutableQuadViewImpl quad) {
        final float brightness = this.blockInfo.blockView.getBrightness(quad.lightFace(), quad.hasShade());

        if ((quad.geometryFlags() & GeometryHelper.AXIS_ALIGNED_FLAG) == 0 || quad.hasVertexNormals()) {
            // Quads that aren't direction-aligned or that have vertex normals need to be shaded
            // using interpolation - vanilla can't handle them. Generally only applies to modded models.
            for (int i = 0; i < 4; i++) {
                quad.spriteColor(i, DEFAULT_TEXTURE_INDEX, ColorARGB.mulRGB(quad.spriteColor(i, DEFAULT_TEXTURE_INDEX), this.calculateVertexShade(quad, i, brightness)));
            }
        } else if (brightness != 1.0f) {
            for (int i = 0; i < 4; i++) {
                quad.spriteColor(i, DEFAULT_TEXTURE_INDEX, ColorARGB.mulRGB(quad.spriteColor(i, DEFAULT_TEXTURE_INDEX), brightness));
            }
        }
    }

    private float calculateVertexShade(MutableQuadViewImpl quad, int vertexIndex, float fallbackBrightness) {
        if (!quad.hasNormal(vertexIndex)) {
            return fallbackBrightness;
        }

        return this.calculateVertexShade(quad, vertexIndex);
    }

    private float calculateVertexShade(MutableQuadViewImpl quad, int vertexIndex) {
        float normalX = quad.normalX(vertexIndex);
        float normalY = quad.normalY(vertexIndex);
        float normalZ = quad.normalZ(vertexIndex);

        boolean shaded = quad.hasShade();

        float sum = 0;
        float div = 0;

        if (normalX > 0) {
            sum += normalX * this.blockInfo.blockView.getBrightness(Direction.EAST, shaded);
            div += normalX;
        } else if (normalX < 0) {
            sum += -normalX * this.blockInfo.blockView.getBrightness(Direction.WEST, shaded);
            div -= normalX;
        }

        if (normalY > 0) {
            sum += normalY * this.blockInfo.blockView.getBrightness(Direction.UP, shaded);
            div += normalY;
        } else if (normalY < 0) {
            sum += -normalY * this.blockInfo.blockView.getBrightness(Direction.DOWN, shaded);
            div -= normalY;
        }

        if (normalZ > 0) {
            sum += normalZ * this.blockInfo.blockView.getBrightness(Direction.SOUTH, shaded);
            div += normalZ;
        } else if (normalZ < 0) {
            sum += -normalZ * this.blockInfo.blockView.getBrightness(Direction.NORTH, shaded);
            div -= normalZ;
        }

        return sum / div;
    }

    /**
     * Where we handle all pre-buffer coloring, lighting, transformation, etc.
     * Reused for all mesh quads. Fixed baking array sized to hold largest possible mesh quad.
     */
    private class Maker extends MutableQuadViewImpl implements QuadEmitter {
        {
            this.data = new int[EncodingFormat.TOTAL_STRIDE];
            this.material(SodiumRenderer.MATERIAL_STANDARD);
        }

        // only used via RenderContext.getEmitter()
        @Override
        public Maker emit() {
            this.computeGeometry();
            AbstractRenderer.this.renderQuad(this);
            this.clear();
            return this;
        }
    }

    private final Maker editorQuad = new Maker();

    public void acceptFabricMesh(Mesh mesh) {
        final MeshImpl m = (MeshImpl) mesh;
        final int[] data = m.data();
        final int limit = data.length;
        int index = 0;

        while (index < limit) {
            System.arraycopy(data, index, this.editorQuad.data(), 0, EncodingFormat.TOTAL_STRIDE);
            this.editorQuad.load();
            index += EncodingFormat.TOTAL_STRIDE;
            this.renderQuad(this.editorQuad);
        }
    }

    public QuadEmitter getEmitter() {
        this.editorQuad.clear();
        return this.editorQuad;
    }

    private void renderQuad(MutableQuadViewImpl quad) {
        if (!this.transform.transform(this.editorQuad) || this.blockInfo.shouldCullFace(quad.cullFace())) {
            return;
        }

        final RenderMaterialValue material = quad.material();

        if (!material.disableAo(0) && MinecraftClient.isAmbientOcclusionEnabled()) {
            // needs to happen before offsets are applied
            this.lighter.compute(quad);
        }

        this.tessellateQuad(quad, material, DEFAULT_TEXTURE_INDEX);
    }

    /**
     * Determines color index and render layer, then routes to appropriate
     * tessellate routine based on material properties.
     */
    private void tessellateQuad(MutableQuadViewImpl quad, RenderMaterialValue mat, int textureIndex) {
        final int colorIndex = mat.disableColorIndex(textureIndex) ? -1 : quad.colorIndex();
        final BlendMode blendMode = mat.blendMode(textureIndex);

        if (this.blockInfo.getAmbientOcclusionDefault() && !mat.disableAo(textureIndex)) {
            if (mat.emissive(textureIndex)) {
                this.tessellateSmoothEmissive(quad, blendMode, colorIndex);
            } else {
                this.tessellateSmooth(quad, blendMode, colorIndex);
            }
        } else {
            if (mat.emissive(textureIndex)) {
                this.tessellateFlatEmissive(quad, blendMode, colorIndex);
            } else {
                this.tessellateFlat(quad, blendMode, colorIndex);
            }
        }
    }

    private static final RenderMaterialValue MATERIAL_FLAT = (RenderMaterialValue) SodiumRenderer.INSTANCE.materialFinder().disableAo(0, true).find();
    private static final RenderMaterialValue MATERIAL_SHADED = (RenderMaterialValue) SodiumRenderer.INSTANCE.materialFinder().find();

    public void renderVanillaModel(BakedModel model) {
        final Supplier<Random> random = this.blockInfo.getRandomSupplier();
        final BlockState blockState = this.blockInfo.blockState;
        
        final RenderMaterialValue defaultMaterial = this.getDefaultVanillaMaterial(model);

        for (Direction cullFace : DirectionUtil.ALL_DIRECTIONS) {
            this.renderVanillaModelFace(model, blockState, cullFace, random, defaultMaterial);
        }

        this.renderVanillaModelFace(model, blockState, null, random, defaultMaterial);
    }

    private void renderVanillaModelFace(BakedModel model, BlockState blockState, Direction cullFace, Supplier<Random> random, RenderMaterialValue defaultMaterial) {
        final List<BakedQuad> quads = model.getQuads(blockState, cullFace, random.get());
        final int count = quads.size();

        //noinspection ForLoopReplaceableByForEach
        for (int j = 0; j < count; j++) {
            this.renderVanillaQuad(quads.get(j), cullFace, defaultMaterial);
        }
    }

    private RenderMaterialValue getDefaultVanillaMaterial(BakedModel model) {
        return (this.blockInfo.getAmbientOcclusionDefault() && model.useAmbientOcclusion()) ? MATERIAL_SHADED : MATERIAL_FLAT;
    }

    private void renderVanillaQuad(BakedQuad quad, Direction cullFace, RenderMaterialValue defaultMaterial) {
        final MutableQuadViewImpl editorQuad = this.editorQuad;
        editorQuad.fromVanilla(quad, defaultMaterial, cullFace);

        if (!this.transform.transform(editorQuad)) {
            return;
        }

        cullFace = editorQuad.cullFace();

        if (cullFace != null && this.blockInfo.shouldCullFace(cullFace)) {
            return;
        }

        if (!editorQuad.material().disableAo(0)) {
            // needs to happen before offsets are applied
            this.lighter.compute(editorQuad);
            this.tessellateSmooth(editorQuad, this.blockInfo.getDefaultLayer(), editorQuad.colorIndex());
        } else {
            // Recomputing whether the quad has a light face is only needed if it doesn't also have a cull face,
            // as in those cases, the cull face will always be used to offset the light sampling position
            if (cullFace == null) {
                // Can't rely on lazy computation in tessellateFlat() because needs to happen before offsets are applied
                editorQuad.geometryFlags();
            }

            this.tessellateFlat(editorQuad, this.blockInfo.getDefaultLayer(), editorQuad.colorIndex());
        }
    }
}
