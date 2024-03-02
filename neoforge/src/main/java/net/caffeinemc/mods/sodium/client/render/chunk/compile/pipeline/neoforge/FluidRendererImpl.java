package net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.neoforge;

import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.neoforge.ForgeColorProviders;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.textures.FluidSpriteCache;

public class FluidRendererImpl extends FluidRenderer {
    // The current default context is set up before invoking FluidRenderHandler#renderFluid and cleared afterwards.
    private static final ThreadLocal<DefaultRenderContext> CURRENT_DEFAULT_CONTEXT = ThreadLocal.withInitial(DefaultRenderContext::new);

    private final ColorProviderRegistry colorProviderRegistry;
    private final DefaultFluidRenderer defaultRenderer;

    public FluidRendererImpl(ColorProviderRegistry colorProviderRegistry, LightPipelineProvider lighters) {
        this.colorProviderRegistry = colorProviderRegistry;
        defaultRenderer = new DefaultFluidRenderer(colorProviderRegistry, lighters);
    }

    public static boolean renderFromVanilla() {
        return CURRENT_DEFAULT_CONTEXT.get().renderIfSetUp();
    }

    public static FluidRenderer create(ColorProviderRegistry colorRegistry, LightPipelineProvider lightPipelineProvider) {
        return new FluidRendererImpl(colorRegistry, lightPipelineProvider);
    }

    public void render(LevelSlice level, BlockState blockState, FluidState fluidState, BlockPos blockPos, BlockPos offset, TranslucentGeometryCollector collector, ChunkBuildBuffers buffers) {
        var material = DefaultMaterials.forFluidState(fluidState);
        var meshBuilder = buffers.get(material);

        IClientFluidTypeExtensions handler = IClientFluidTypeExtensions.of(fluidState);

        // Match the vanilla FluidRenderer's behavior if the handler is null
        // The IDE's wrong. Ignore it.
        if (handler.getStillTexture() == null) {
            boolean isLava = fluidState.is(FluidTags.LAVA);
            handler = IClientFluidTypeExtensions.of(isLava ? Fluids.LAVA : Fluids.WATER);
        }

        // Invoking FluidRenderHandler#renderFluid can invoke vanilla FluidRenderer#render.
        //
        // Sodium cannot let vanilla FluidRenderer#render run (during the invocation of FluidRenderHandler#renderFluid)
        // for two reasons.
        // 1. It is the hot path and vanilla FluidRenderer#render is not very fast.
        // 2. Fabric API's mixins to FluidRenderer#render expect it to be initially called from the chunk rebuild task,
        // not from inside FluidRenderHandler#renderFluid. Not upholding this assumption will result in all custom
        // geometry to be buffered twice.
        //
        // The default implementation of FluidRenderHandler#renderFluid invokes vanilla FluidRenderer#render, but
        // Fabric API does not support invoking vanilla FluidRenderer#render from FluidRenderHandler#renderFluid
        // directly and it does not support calling the default implementation of FluidRenderHandler#renderFluid (super)
        // more than once. Because of this, the parameters to vanilla FluidRenderer#render will be the same as those
        // initially passed to FluidRenderHandler#renderFluid, so they can be ignored.
        //
        // Due to all the above, Sodium injects into head of vanilla FluidRenderer#render before Fabric API and cancels
        // the call if it was invoked from inside FluidRenderHandler#renderFluid. The injector ends up calling
        // DefaultFluidRenderer#render, which emulates what vanilla FluidRenderer#render does, but is more efficient.
        // To allow invoking this method from the injector, where there is no local Sodium context, the renderer and
        // parameters are bundled into a DefaultRenderContext which is stored in a ThreadLocal.

        DefaultRenderContext defaultContext = CURRENT_DEFAULT_CONTEXT.get();
        defaultContext.setUp(this.colorProviderRegistry, this.defaultRenderer, level, fluidState, blockPos, offset, collector, meshBuilder, material, handler);

        try {
            if (!handler.renderFluid(fluidState, level, blockPos, meshBuilder.asFallbackVertexConsumer(material, collector), blockState)) {
                defaultContext.renderIfSetUp();
            }
        } finally {
            defaultContext.clear();
        }
    }

    private static class DefaultRenderContext {
        private DefaultFluidRenderer renderer;
        private LevelSlice level;
        private FluidState fluidState;
        private BlockPos blockPos;
        private BlockPos offset;
        private TranslucentGeometryCollector collector;
        private ChunkModelBuilder meshBuilder;
        private Material material;
        private IClientFluidTypeExtensions handler;
        private ColorProviderRegistry colorProviderRegistry;

        public void setUp(ColorProviderRegistry colorProviderRegistry, DefaultFluidRenderer renderer, LevelSlice level, FluidState fluidState, BlockPos blockPos, BlockPos offset, TranslucentGeometryCollector collector, ChunkModelBuilder meshBuilder, Material material, IClientFluidTypeExtensions handler) {
            this.colorProviderRegistry = colorProviderRegistry;
            this.renderer = renderer;
            this.level = level;
            this.fluidState = fluidState;
            this.blockPos = blockPos;
            this.offset = offset;
            this.collector = collector;
            this.meshBuilder = meshBuilder;
            this.material = material;
            this.handler = handler;
        }

        public void clear() {
            this.renderer = null;
            this.level = null;
            this.fluidState = null;
            this.blockPos = null;
            this.offset = null;
            this.collector = null;
            this.meshBuilder = null;
            this.material = null;
            this.handler = null;
        }

        public ColorProvider<FluidState> getColorProvider(Fluid fluid) {
            var override = this.colorProviderRegistry.getColorProvider(fluid);

            if (override != null) {
                return override;
            }

            return ForgeColorProviders.adapt(handler);
        }

        public boolean renderIfSetUp() {
            if (this.renderer != null) {
                this.renderer.render(this.level, this.fluidState, this.blockPos, this.offset, this.collector, this.meshBuilder, this.material,
                        getColorProvider(fluidState.getType()), FluidSpriteCache.getFluidSprites(level, blockPos, fluidState));
                return true;
            }

            return false;
        }
    }
}
