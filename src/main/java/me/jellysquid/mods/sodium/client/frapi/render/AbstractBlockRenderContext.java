package me.jellysquid.mods.sodium.client.frapi.render;

import me.jellysquid.mods.sodium.client.frapi.SodiumRenderer;
import me.jellysquid.mods.sodium.client.frapi.mesh.EncodingFormat;
import me.jellysquid.mods.sodium.client.frapi.mesh.MutableQuadViewImpl;
import me.jellysquid.mods.sodium.client.model.light.*;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Base class for the functions that can be shared between the terrain and non-terrain pipelines.
 *
 * <p>Make sure to set the {@link #lighters} in the subclass constructor.
 */
public abstract class AbstractBlockRenderContext extends AbstractRenderContext {
    protected BlockRenderContext ctx;

    /**
     * Pipeline entrypoint - handles transform and culling checks.
     */
    protected final void renderQuad(MutableQuadViewImpl quad) {
        if (!transform(quad)) {
            return;
        }

        if (!isFaceVisible(ctx, quad.cullFace())) {
            return;
        }

        processQuad(quad);
    }

    /**
     * Quad pipeline function - after transform and culling checks.
     * Can also be used as entrypoint to skip some logic if the transform and culling checks have already been performed.
     */
    protected abstract void processQuad(MutableQuadViewImpl quad);

    /* Random handling */
    private final Random random = new LocalRandom(42L);
    protected final Supplier<Random> randomSupplier = () -> prepareRandom(this.ctx);

    protected Random prepareRandom(BlockRenderContext ctx) {
        var random = this.random;
        random.setSeed(ctx.seed());
        return random;
    }

    /* Occlusion handling */
    private final BlockOcclusionCache occlusionCache = new BlockOcclusionCache();
    /**
     * Whether culling is enabled at all.
     */
    private boolean enableCulling = true;
    // Cull cache (as it's checked per-quad instead of once in vanilla)
    private int cullCompletionFlags;
    private int cullResultFlags;

    protected boolean isFaceVisible(BlockRenderContext ctx, @Nullable Direction face) {
        if (face == null || !enableCulling) {
            return true;
        }

        final int mask = 1 << face.getId();

        if ((this.cullCompletionFlags & mask) == 0) {
            this.cullCompletionFlags |= mask;

            if (this.occlusionCache.shouldDrawSide(ctx.state(), ctx.world(), ctx.pos(), face)) {
                this.cullResultFlags |= mask;
                return true;
            } else {
                return false;
            }
        } else {
            return (this.cullResultFlags & mask) != 0;
        }
    }

    protected void resetCullState(boolean enableCulling) {
        this.enableCulling = enableCulling;
        this.cullCompletionFlags = 0;
        this.cullResultFlags = 0;
    }

    /* Shading handling */
    // TODO: is it problematic to cache this value here?
    protected final boolean useAmbientOcclusion = MinecraftClient.isAmbientOcclusionEnabled();
    protected final QuadLightData quadLightData = new QuadLightData();
    /**
     * Must be set by the subclass constructor.
     */
    protected LightPipelineProvider lighters;

    protected LightMode getLightingMode(BlockState state, BakedModel model) {
        if (this.useAmbientOcclusion && model.useAmbientOcclusion() && state.getLuminance() == 0) {
            return LightMode.SMOOTH;
        } else {
            return LightMode.FLAT;
        }
    }

    /**
     * @param lightData Just use {@link #quadLightData}, but pass it as a parameter to avoid a field lookup.
     */
    protected void shadeQuad(BlockRenderContext ctx, MutableQuadViewImpl quad, LightMode lightMode, boolean emissive, QuadLightData lightData) {
        // TODO: do we want normal-based diffuse shading like in Indigo?
        // TODO: do we want to port enhanced AO from Indigo to the smooth pipeline?

        LightPipeline lighter = this.lighters.getLighter(lightMode);
        lighter.calculate(quad, ctx.pos(), lightData, quad.cullFace(), quad.lightFace(), quad.hasShade());

        // routines below have a bit of copy-paste code reuse to avoid conditional execution inside a hot loop
        if (emissive) {
            for (int i = 0; i < 4; i++) {
                quad.lightmap(i, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                quad.lightmap(i, lightData.lm[i]);
            }
        }
    }

    /* Handling of vanilla models - this is the hot path for non-modded models */
    private final BakedModelConsumerImpl bakedModelConsumer = new BakedModelConsumerImpl();

    @Override
    public BakedModelConsumer bakedModelConsumer() {
        return bakedModelConsumer;
    }

    private class BakedModelConsumerImpl implements BakedModelConsumer {
        private static final RenderMaterial MATERIAL_SHADED = SodiumRenderer.INSTANCE.materialFinder().find();
        private static final RenderMaterial MATERIAL_FLAT = SodiumRenderer.INSTANCE.materialFinder().ambientOcclusion(TriState.FALSE).find();

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

        @Override
        public void accept(BakedModel model) {
            accept(model, ctx.state());
        }

        @Override
        public void accept(BakedModel model, @Nullable BlockState state) {
            BlockRenderContext ctx = AbstractBlockRenderContext.this.ctx;

            MutableQuadViewImpl editorQuad = this.editorQuad;
            final RenderMaterial defaultMaterial = model.useAmbientOcclusion() ? MATERIAL_SHADED : MATERIAL_FLAT;

            // If there is no transform, we can check the culling face once for all the quads,
            // and we don't need to check for transforms per-quad.
            boolean noTransform = !hasTransform();

            for (int i = 0; i <= ModelHelper.NULL_FACE_ID; i++) {
                final Direction cullFace = ModelHelper.faceFromIndex(i);
                final List<BakedQuad> quads = model.getQuads(state, cullFace, prepareRandom(ctx));

                if (!quads.isEmpty()) {
                    renderQuadList(ctx, editorQuad, noTransform, defaultMaterial, cullFace, quads);
                }
            }

            // Do not clear the editorQuad since it is not accessible to API users.
        }

        private void renderQuadList(BlockRenderContext ctx, MutableQuadViewImpl editorQuad, boolean noTransform, RenderMaterial defaultMaterial, @Nullable Direction cullFace, List<BakedQuad> quads) {
            if (noTransform) {
                if (!isFaceVisible(ctx, cullFace)) {
                    return;
                }

                int count = quads.size();

                for (int j = 0; j < count; j++) {
                    final BakedQuad q = quads.get(j);
                    editorQuad.fromVanilla(q, defaultMaterial, cullFace);
                    // Call processQuad directly for efficiency
                    processQuad(editorQuad);
                }
            } else {
                int count = quads.size();

                for (int j = 0; j < count; j++) {
                    final BakedQuad q = quads.get(j);
                    editorQuad.fromVanilla(q, defaultMaterial, cullFace);
                    // Call renderQuad directly instead of emit for efficiency
                    renderQuad(editorQuad);
                }
            }
        }
    }
}
