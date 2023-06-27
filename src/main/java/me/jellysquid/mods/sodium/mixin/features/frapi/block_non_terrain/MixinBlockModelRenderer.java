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

package me.jellysquid.mods.sodium.mixin.features.frapi.block_non_terrain;

import me.jellysquid.mods.sodium.client.frapi.render.BlockModelRendererExtended;
import me.jellysquid.mods.sodium.client.frapi.render.NonTerrainBlockRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entrypoint of the FRAPI pipeline for non-terrain block rendering, for the baked models that require it.
 */
@Mixin(BlockModelRenderer.class)
public abstract class MixinBlockModelRenderer implements BlockModelRendererExtended {
	@Unique
	private ThreadLocal<NonTerrainBlockRenderContext> sodium_contexts = ThreadLocal.withInitial(NonTerrainBlockRenderContext::new);

	@Inject(at = @At("HEAD"), method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)V", cancellable = true)
	private void hookRender(BlockRenderView blockView, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrix, VertexConsumer buffer, boolean cull, Random rand, long seed, int overlay, CallbackInfo ci) {
		if (!model.isVanillaAdapter()) {
            NonTerrainBlockRenderContext context = sodium_contexts.get();
			context.renderModel(blockView, model, state, pos, matrix, buffer, cull, rand, seed, overlay);
			ci.cancel();
		}
	}

    @Override
    public void sodium_clearFrapiContexts() {
        this.sodium_contexts = ThreadLocal.withInitial(NonTerrainBlockRenderContext::new);
    }
}
