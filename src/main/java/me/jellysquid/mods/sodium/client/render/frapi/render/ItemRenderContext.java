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

import me.jellysquid.mods.sodium.client.render.frapi.SpriteFinderCache;
import me.jellysquid.mods.sodium.client.render.frapi.helper.ColorHelper;
import me.jellysquid.mods.sodium.client.render.frapi.mesh.EncodingFormat;
import me.jellysquid.mods.sodium.client.render.frapi.mesh.MutableQuadViewImpl;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.fabric.impl.renderer.VanillaModelEncoder;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.function.Supplier;

/**
 * The render context used for item rendering.
 */
public class ItemRenderContext extends AbstractRenderContext {
    /** Value vanilla uses for item rendering.  The only sensible choice, of course.  */
    private static final long ITEM_RANDOM_SEED = 42L;

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

    @Deprecated
    private final BakedModelConsumerImpl vanillaModelConsumer = new BakedModelConsumerImpl();

    private final ItemColors colorMap;
    private final VanillaModelBufferer vanillaBufferer;

    private final Random random = new LocalRandom(ITEM_RANDOM_SEED);
    private final Supplier<Random> randomSupplier = () -> {
        random.setSeed(ITEM_RANDOM_SEED);
        return random;
    };

    private ItemStack itemStack;
    private ModelTransformationMode transformMode;
    private MatrixStack matrixStack;
    private Matrix4f matPosition;
    private Matrix3f matNormal;
    private VertexConsumerProvider vertexConsumerProvider;
    private int lightmap;
    private int overlay;

    private boolean isDefaultTranslucent;
    private boolean isTranslucentDirect;
    private boolean isDefaultGlint;

    private VertexConsumer translucentVertexConsumer;
    private VertexConsumer cutoutVertexConsumer;
    private VertexConsumer translucentGlintVertexConsumer;
    private VertexConsumer cutoutGlintVertexConsumer;
    private VertexConsumer defaultVertexConsumer;

    public ItemRenderContext(ItemColors colorMap, VanillaModelBufferer vanillaBufferer) {
        this.colorMap = colorMap;
        this.vanillaBufferer = vanillaBufferer;
    }

    @Override
    public QuadEmitter getEmitter() {
        editorQuad.clear();
        return editorQuad;
    }

    @Override
    public boolean isFaceCulled(@Nullable Direction face) {
        throw new UnsupportedOperationException("isFaceCulled can only be called on a block render context.");
    }

    @Override
    public ModelTransformationMode itemTransformationMode() {
        return transformMode;
    }

    @Deprecated
    @Override
    public BakedModelConsumer bakedModelConsumer() {
        return vanillaModelConsumer;
    }

    public void renderModel(ItemStack itemStack, ModelTransformationMode transformMode, boolean invert, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int lightmap, int overlay, BakedModel model) {
        this.itemStack = itemStack;
        this.transformMode = transformMode;
        this.matrixStack = matrixStack;
        matPosition = matrixStack.peek().getPositionMatrix();
        matNormal = matrixStack.peek().getNormalMatrix();
        this.vertexConsumerProvider = vertexConsumerProvider;
        this.lightmap = lightmap;
        this.overlay = overlay;
        computeOutputInfo();

        model.emitItemQuads(itemStack, randomSupplier, this);

        this.itemStack = null;
        this.vertexConsumerProvider = null;

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
        QuadEncoder.writeQuadVertices(quad, vertexConsumer, overlay, matPosition, matNormal);
        SpriteUtil.markSpriteActive(quad.sprite(SpriteFinderCache.forBlockAtlas()));
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

    public void bufferDefaultModel(BakedModel model, @Nullable BlockState state) {
        if (hasTransform()) {
            VanillaModelEncoder.emitItemQuads(model, state, randomSupplier, ItemRenderContext.this);
        } else {
            vanillaBufferer.accept(model, itemStack, lightmap, overlay, matrixStack, defaultVertexConsumer);
        }
    }

    @Deprecated
    private class BakedModelConsumerImpl implements BakedModelConsumer {
        @Override
        public void accept(BakedModel model) {
            accept(model, null);
        }

        @Override
        public void accept(BakedModel model, @Nullable BlockState state) {
            bufferDefaultModel(model, state);
        }
    }

    /** used to accept a method reference from the ItemRenderer. */
    @FunctionalInterface
    public interface VanillaModelBufferer {
        void accept(BakedModel model, ItemStack stack, int color, int overlay, MatrixStack matrixStack, VertexConsumer buffer);
    }
}
