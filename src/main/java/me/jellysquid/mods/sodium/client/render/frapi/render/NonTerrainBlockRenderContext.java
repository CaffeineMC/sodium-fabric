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

package me.jellysquid.mods.sodium.client.render.frapi.render;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.SingleBlockLightDataCache;
import me.jellysquid.mods.sodium.client.render.frapi.SpriteFinderCache;
import me.jellysquid.mods.sodium.client.render.frapi.helper.ColorHelper;
import me.jellysquid.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class NonTerrainBlockRenderContext extends AbstractBlockRenderContext {
    private final BlockColors colorMap;
    private final SingleBlockLightDataCache lightDataCache = new SingleBlockLightDataCache();

    private VertexConsumer vertexConsumer;
    private Matrix4f matPosition;
    private Matrix3f matNormal;
    private int overlay;

    public NonTerrainBlockRenderContext(BlockColors colorMap) {
        this.colorMap = colorMap;
        this.lighters = new LightPipelineProvider(this.lightDataCache);
    }

    public void renderModel(BlockRenderView blockView, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrixStack, VertexConsumer buffer, boolean cull, Random random, long seed, int overlay) {
        this.world = blockView;
        this.state = state;
        this.pos = pos;

        this.random = random;
        this.randomSeed = seed;

        this.vertexConsumer = buffer;
        this.matPosition = matrixStack.peek().getPositionMatrix();
        this.matNormal = matrixStack.peek().getNormalMatrix();
        this.overlay = overlay;

        this.lightDataCache.reset(pos, blockView);
        this.prepareCulling(cull);
        this.prepareAoInfo(model.useAmbientOcclusion());

        model.emitBlockQuads(blockView, state, pos, this.randomSupplier, this);

        this.world = null;
        this.lightDataCache.release();
        this.random = null;
        this.vertexConsumer = null;
    }

    @Override
    protected void processQuad(MutableQuadViewImpl quad) {
        final RenderMaterial mat = quad.material();
        final int colorIndex = mat.disableColorIndex() ? -1 : quad.colorIndex();
        final TriState aoMode = mat.ambientOcclusion();
        final LightMode lightMode;
        if (aoMode == TriState.DEFAULT) {
            lightMode = this.defaultLightMode;
        } else {
            lightMode = this.useAmbientOcclusion && aoMode.get() ? LightMode.SMOOTH : LightMode.FLAT;
        }
        final boolean emissive = mat.emissive();

        colorizeQuad(quad, colorIndex);
        shadeQuad(quad, lightMode, emissive);
        bufferQuad(quad);
    }

    private void colorizeQuad(MutableQuadViewImpl quad, int colorIndex) {
        if (colorIndex != -1) {
            final int blockColor = 0xFF000000 | this.colorMap.getColor(this.state, this.world, this.pos, colorIndex);

            for (int i = 0; i < 4; i++) {
                quad.color(i, ColorHelper.multiplyColor(blockColor, quad.color(i)));
            }
        }
    }

    @Override
    protected void shadeQuad(MutableQuadViewImpl quad, LightMode lightMode, boolean emissive) {
        super.shadeQuad(quad, lightMode, emissive);

        float[] brightnesses = this.quadLightData.br;

        for (int i = 0; i < 4; i++) {
            quad.color(i, ColorHelper.multiplyRGB(quad.color(i), brightnesses[i]));
        }
    }

    private void bufferQuad(MutableQuadViewImpl quad) {
        QuadEncoder.writeQuadVertices(quad, vertexConsumer, overlay, matPosition, matNormal);
        SpriteUtil.markSpriteActive(quad.sprite(SpriteFinderCache.forBlockAtlas()));
    }
}
