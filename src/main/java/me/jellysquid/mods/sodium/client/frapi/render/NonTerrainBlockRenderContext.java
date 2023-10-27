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

package me.jellysquid.mods.sodium.client.frapi.render;

import me.jellysquid.mods.sodium.client.frapi.helper.ColorHelper;
import me.jellysquid.mods.sodium.client.frapi.mesh.EncodingFormat;
import me.jellysquid.mods.sodium.client.frapi.mesh.MutableQuadViewImpl;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.SingleBlockLightDataCache;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class NonTerrainBlockRenderContext extends AbstractBlockRenderContext {
    private final BlockColors blockColorMap = MinecraftClient.getInstance().getBlockColors();
    private final SingleBlockLightDataCache lightDataCache = new SingleBlockLightDataCache();

    // Holders for state used in FRAPI as we can't pass them via parameters
    private VertexBufferWriter vertexWriter;
    private MatrixStack.Entry matrixEntry;
    private int overlay;
    // Default AO mode for model (can be overridden by material property)
    private LightMode defaultLightMode;

	private final MutableQuadViewImpl editorQuad = new MutableQuadViewImpl() {
		{
			data = new int[EncodingFormat.TOTAL_STRIDE];
			clear();
		}

		@Override
		public void emitDirectly() {
			renderQuad(this);
		}
	};

    public NonTerrainBlockRenderContext() {
        this.lighters = new LightPipelineProvider(this.lightDataCache);
        this.ctx = new BlockRenderContext(null);
    }

    public void renderModel(BlockRenderView blockView, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrixStack, VertexConsumer buffer, boolean cull, Random random, long seed, int overlay) {
        // Store parameters
        this.vertexWriter = VertexBufferWriter.of(buffer);
        this.matrixEntry = matrixStack.peek();
        this.overlay = overlay;

        // Clear old state
        this.resetCullState(cull);
        this.lightDataCache.reset(pos, blockView);

        // Prepare
        this.ctx.updateWorld(blockView);
        this.ctx.update(pos, BlockPos.ORIGIN, state, model, seed);
        this.defaultLightMode = this.getLightingMode(ctx.state(), ctx.model());

        // Actually render
        model.emitBlockQuads(blockView, state, pos, this.randomSupplier, this);

        // Avoid dangling references
        this.vertexWriter = null;
        this.ctx.updateWorld(null);
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

        BlockRenderContext ctx = this.ctx;

        colorizeQuad(ctx, quad, colorIndex);
        QuadLightData lightData = this.quadLightData;
        shadeQuad(ctx, quad, lightMode, emissive, lightData);
        applyBrightness(quad, lightData.br);
        bufferQuad(quad);
    }

    private void colorizeQuad(BlockRenderContext ctx, MutableQuadViewImpl quad, int colorIndex) {
        if (colorIndex != -1) {
            final int blockColor = 0xFF000000 | this.blockColorMap.getColor(ctx.state(), ctx.world(), ctx.pos(), colorIndex);

            for (int i = 0; i < 4; i++) {
                quad.color(i, ColorHelper.multiplyColor(blockColor, quad.color(i)));
            }
        }
    }

    private void applyBrightness(MutableQuadViewImpl quad, float[] brightness) {
        for (int i = 0; i < 4; i++) {
            quad.color(i, ColorHelper.multiplyRGB(quad.color(i), brightness[i]));
        }
    }

    private void bufferQuad(MutableQuadViewImpl quad) {
        BakedModelEncoder.writeQuadVertices(this.vertexWriter, this.matrixEntry, quad, this.overlay);

        SpriteUtil.markSpriteActive(quad.getSprite(this.spriteFinder));
    }

    @Override
	public QuadEmitter getEmitter() {
		editorQuad.clear();
		return editorQuad;
	}
}
