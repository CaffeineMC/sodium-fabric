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

import me.jellysquid.mods.sodium.mixin.features.frapi.item.ItemRendererAccessor;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class VanillaQuadHandlerImpl implements ItemRenderContext.VanillaQuadHandler {
	private final ItemRendererAccessor itemRenderer;

	public VanillaQuadHandlerImpl(ItemRenderer itemRenderer) {
		this.itemRenderer = (ItemRendererAccessor) itemRenderer;
	}

	@Override
	public void accept(BakedModel model, ItemStack stack, int color, int overlay, MatrixStack matrixStack, VertexConsumer buffer) {
		itemRenderer.callRenderBakedItemModel(model, stack, color, overlay, matrixStack, buffer);
	}
}
