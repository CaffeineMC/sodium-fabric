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

import me.jellysquid.mods.sodium.client.frapi.SodiumRenderer;
import me.jellysquid.mods.sodium.client.frapi.helper.ColorHelper;
import me.jellysquid.mods.sodium.client.frapi.mesh.EncodingFormat;
import me.jellysquid.mods.sodium.client.frapi.mesh.MutableQuadViewImpl;
import me.jellysquid.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * The render context used for item rendering.
 */
public class ItemRenderContext extends AbstractRenderContext {
	/** Value vanilla uses for item rendering.  The only sensible choice, of course.  */
	private static final long ITEM_RANDOM_SEED = 42L;

    /* Random handling */
	private final Random random = Random.create();
	private final Supplier<Random> randomSupplier = this::prepareRandom;

    protected Random prepareRandom() {
        var random = this.random;
        random.setSeed(ITEM_RANDOM_SEED);
        return random;
    }

    private final ItemColors colorMap;

	private ItemStack itemStack;
	private ModelTransformationMode transformMode;
	private MatrixStack matrixStack;
    private MatrixStack.Entry matrixEntry;
	private VertexConsumerProvider vertexConsumerProvider;
	private int lightmap;
    private int overlay;
	private VanillaQuadHandler vanillaHandler;

	private boolean isDefaultTranslucent;
	private boolean isTranslucentDirect;
	private boolean isDefaultGlint;

	private VertexConsumer translucentVertexConsumer;
	private VertexConsumer cutoutVertexConsumer;
	private VertexConsumer translucentGlintVertexConsumer;
	private VertexConsumer cutoutGlintVertexConsumer;
	private VertexConsumer defaultVertexConsumer;

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
    private final BakedModelConsumerImpl bakedModelConsumer = new BakedModelConsumerImpl();

	public ItemRenderContext(ItemColors colorMap) {
		this.colorMap = colorMap;
	}

	@Override
	public QuadEmitter getEmitter() {
		editorQuad.clear();
		return editorQuad;
	}

	@Override
	public BakedModelConsumer bakedModelConsumer() {
		return bakedModelConsumer;
	}

	public void renderModel(ItemStack itemStack, ModelTransformationMode transformMode, boolean invert, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int lightmap, int overlay, BakedModel model, VanillaQuadHandler vanillaHandler) {
		this.itemStack = itemStack;
		this.transformMode = transformMode;
		this.matrixStack = matrixStack;
        this.matrixEntry = matrixStack.peek();
		this.vertexConsumerProvider = vertexConsumerProvider;
		this.lightmap = lightmap;
		this.overlay = overlay;
		this.vanillaHandler = vanillaHandler;
		computeOutputInfo();

		model.emitItemQuads(itemStack, randomSupplier, this);

		this.itemStack = null;
		this.matrixStack = null;
		this.vertexConsumerProvider = null;
		this.vanillaHandler = null;

		translucentVertexConsumer = null;
		cutoutVertexConsumer = null;
		translucentGlintVertexConsumer = null;
		cutoutGlintVertexConsumer = null;
		defaultVertexConsumer = null;
	}

	private void computeOutputInfo() {
		isDefaultTranslucent = true;
		isTranslucentDirect = true;

		Item item = itemStack.getItem();

		if (item instanceof BlockItem blockItem) {
			BlockState state = blockItem.getBlock().getDefaultState();
			RenderLayer renderLayer = RenderLayers.getBlockLayer(state);

			if (renderLayer != RenderLayer.getTranslucent()) {
				isDefaultTranslucent = false;
			}

			if (transformMode != ModelTransformationMode.GUI && !transformMode.isFirstPerson()) {
				isTranslucentDirect = false;
			}
		}

		isDefaultGlint = itemStack.hasGlint();

		defaultVertexConsumer = getVertexConsumer(BlendMode.DEFAULT, TriState.DEFAULT);
	}

	private void renderQuad(MutableQuadViewImpl quad) {
		if (!transform(quad)) {
			return;
		}

		final RenderMaterial mat = quad.material();
		final int colorIndex = mat.disableColorIndex() ? -1 : quad.colorIndex();
		final boolean emissive = mat.emissive();
		final VertexConsumer vertexConsumer = getVertexConsumer(mat.blendMode(), mat.glint());

		colorizeQuad(quad, colorIndex);
		shadeQuad(quad, emissive);
		bufferQuad(quad, vertexConsumer);
	}

	private void colorizeQuad(MutableQuadViewImpl quad, int colorIndex) {
		if (colorIndex != -1) {
			final int itemColor = 0xFF000000 | colorMap.getColor(itemStack, colorIndex);

			for (int i = 0; i < 4; i++) {
				quad.color(i, ColorHelper.multiplyColor(itemColor, quad.color(i)));
			}
		}
	}

	private void shadeQuad(MutableQuadViewImpl quad, boolean emissive) {
		if (emissive) {
			for (int i = 0; i < 4; i++) {
				quad.lightmap(i, LightmapTextureManager.MAX_LIGHT_COORDINATE);
			}
		} else {
			final int lightmap = this.lightmap;

			for (int i = 0; i < 4; i++) {
				quad.lightmap(i, ColorHelper.maxBrightness(quad.lightmap(i), lightmap));
			}
		}
	}

    private void bufferQuad(MutableQuadViewImpl quad, VertexConsumer vertexConsumer) {
        var writer = VertexBufferWriter.of(vertexConsumer);
        BakedModelEncoder.writeQuadVertices(writer, matrixEntry, quad, overlay);

        SpriteUtil.markSpriteActive(quad.getSprite(spriteFinder));
    }

	/**
	 * Caches custom blend mode / vertex consumers and mimics the logic
	 * in {@code RenderLayers.getEntityBlockLayer}. Layers other than
	 * translucent are mapped to cutout.
	 */
	private VertexConsumer getVertexConsumer(BlendMode blendMode, TriState glintMode) {
		boolean translucent;
		boolean glint;

		if (blendMode == BlendMode.DEFAULT) {
			translucent = isDefaultTranslucent;
		} else {
			translucent = blendMode == BlendMode.TRANSLUCENT;
		}

		if (glintMode == TriState.DEFAULT) {
			glint = isDefaultGlint;
		} else {
			glint = glintMode == TriState.TRUE;
		}

		if (translucent) {
			if (glint) {
				if (translucentGlintVertexConsumer == null) {
					translucentGlintVertexConsumer = createTranslucentVertexConsumer(true);
				}

				return translucentGlintVertexConsumer;
			} else {
				if (translucentVertexConsumer == null) {
					translucentVertexConsumer = createTranslucentVertexConsumer(false);
				}

				return translucentVertexConsumer;
			}
		} else {
			if (glint) {
				if (cutoutGlintVertexConsumer == null) {
					cutoutGlintVertexConsumer = createCutoutVertexConsumer(true);
				}

				return cutoutGlintVertexConsumer;
			} else {
				if (cutoutVertexConsumer == null) {
					cutoutVertexConsumer = createCutoutVertexConsumer(false);
				}

				return cutoutVertexConsumer;
			}
		}
	}

	private VertexConsumer createTranslucentVertexConsumer(boolean glint) {
		if (isTranslucentDirect) {
			return ItemRenderer.getDirectItemGlintConsumer(vertexConsumerProvider, TexturedRenderLayers.getEntityTranslucentCull(), true, glint);
		} else if (MinecraftClient.isFabulousGraphicsOrBetter()) {
			return ItemRenderer.getItemGlintConsumer(vertexConsumerProvider, TexturedRenderLayers.getItemEntityTranslucentCull(), true, glint);
		} else {
			return ItemRenderer.getItemGlintConsumer(vertexConsumerProvider, TexturedRenderLayers.getEntityTranslucentCull(), true, glint);
		}
	}

	private VertexConsumer createCutoutVertexConsumer(boolean glint) {
		return ItemRenderer.getDirectItemGlintConsumer(vertexConsumerProvider, TexturedRenderLayers.getEntityCutout(), true, glint);
	}

	private class BakedModelConsumerImpl implements BakedModelConsumer {
		@Override
		public void accept(BakedModel model) {
			accept(model, null);
		}

		@Override
		public void accept(BakedModel model, @Nullable BlockState state) {
			if (hasTransform()) {
				MutableQuadViewImpl editorQuad = ItemRenderContext.this.editorQuad;

				// if there's a transform in effect, convert to mesh-based quads so that we can apply it
				for (int i = 0; i <= ModelHelper.NULL_FACE_ID; i++) {
					final Direction cullFace = ModelHelper.faceFromIndex(i);
					final List<BakedQuad> quads = model.getQuads(state, cullFace, prepareRandom());
					final int count = quads.size();

					for (int j = 0; j < count; j++) {
						final BakedQuad q = quads.get(j);
						editorQuad.fromVanilla(q, SodiumRenderer.MATERIAL_STANDARD, cullFace);
						// Call renderQuad directly instead of emit for efficiency
						renderQuad(editorQuad);
					}
				}

				editorQuad.clear();
			} else {
				vanillaHandler.accept(model, itemStack, lightmap, overlay, matrixStack, defaultVertexConsumer);
			}
		}
	}

	/** used to accept a method reference from the ItemRenderer. */
	@FunctionalInterface
	public interface VanillaQuadHandler {
		void accept(BakedModel model, ItemStack stack, int color, int overlay, MatrixStack matrixStack, VertexConsumer buffer);
	}
}
