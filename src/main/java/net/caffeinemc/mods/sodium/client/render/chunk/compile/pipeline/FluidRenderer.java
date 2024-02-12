package net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline;

import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class FluidRenderer {
    // The current default context is set up before invoking FluidRenderHandler#renderFluid and cleared afterwards.
    private static final ThreadLocal<DefaultRenderContext> CURRENT_DEFAULT_CONTEXT = ThreadLocal.withInitial(DefaultRenderContext::new);

    private final DefaultFluidRenderer defaultRenderer;

    public FluidRenderer(ColorProviderRegistry colorProviderRegistry, LightPipelineProvider lighters) {
        defaultRenderer = new DefaultFluidRenderer(colorProviderRegistry, lighters);
    }

    public void render(LevelSlice level, BlockState blockState, FluidState fluidState, BlockPos blockPos, BlockPos offset, ChunkBuildBuffers buffers) {
        var material = DefaultMaterials.forFluidState(fluidState);
        var meshBuilder = buffers.get(material);

        FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluidState.getType());

        // Match the vanilla FluidRenderer's behavior if the handler is null
        if (handler == null) {
            boolean isLava = fluidState.is(FluidTags.LAVA);
            handler = FluidRenderHandlerRegistry.INSTANCE.get(isLava ? Fluids.LAVA : Fluids.WATER);
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
        defaultContext.setUp(this.defaultRenderer, level, fluidState, blockPos, offset, meshBuilder, material, handler);

        try {
            handler.renderFluid(blockPos, level, meshBuilder.asFallbackVertexConsumer(material), blockState, fluidState);
        } finally {
            defaultContext.clear();
        }
    }

    public static boolean renderFromVanilla() {
        return CURRENT_DEFAULT_CONTEXT.get().renderIfSetUp();
    }

    private static class DefaultRenderContext {
        private DefaultFluidRenderer renderer;
        private LevelSlice level;
        private FluidState fluidState;
        private BlockPos blockPos;
        private BlockPos offset;
        private ChunkModelBuilder meshBuilder;
        private Material material;
        private FluidRenderHandler handler;

        public void setUp(DefaultFluidRenderer renderer, LevelSlice level, FluidState fluidState, BlockPos blockPos, BlockPos offset, ChunkModelBuilder meshBuilder, Material material, FluidRenderHandler handler) {
            this.renderer = renderer;
            this.level = level;
            this.fluidState = fluidState;
            this.blockPos = blockPos;
            this.offset = offset;
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
            this.meshBuilder = null;
            this.material = null;
            this.handler = null;
        }

        public boolean renderIfSetUp() {
            if (this.renderer != null) {
                this.renderer.render(this.level, this.fluidState, this.blockPos, this.offset, this.meshBuilder, this.material, this.handler);
                return true;
            }

            return false;
        }
    }
}
