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

package net.caffeinemc.mods.sodium.mixin.features.render.frapi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.client.render.frapi.render.NonTerrainBlockRenderContext;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entrypoint of the FRAPI pipeline for non-terrain block rendering, for the baked models that require it.
 */
@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererMixin {
    @Shadow
    @Final
    private BlockColors blockColors;

    @Unique
    private final ThreadLocal<NonTerrainBlockRenderContext> contexts = ThreadLocal.withInitial(() -> new NonTerrainBlockRenderContext(blockColors));

    @Inject(method = "tesselateBlock", at = @At("HEAD"), cancellable = true)
    private void onRender(BlockAndTintGetter blockView, BakedModel model, BlockState state, BlockPos pos, PoseStack matrix, VertexConsumer buffer, boolean cull, RandomSource rand, long seed, int overlay, CallbackInfo ci) {
        if (!((FabricBakedModel) model).isVanillaAdapter()) {
            contexts.get().renderModel(blockView, model, state, pos, matrix, buffer, cull, rand, seed, overlay);
            ci.cancel();
        }
    }
}
