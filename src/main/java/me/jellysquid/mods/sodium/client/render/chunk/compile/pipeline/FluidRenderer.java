package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import me.jellysquid.mods.sodium.client.model.color.ColorProviderRegistry;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

public class FluidRenderer {
    // The current default context is set up before invoking FluidRenderHandler#renderFluid and cleared afterwards.
    private static final ThreadLocal<DefaultRenderContext> CURRENT_DEFAULT_CONTEXT = ThreadLocal.withInitial(DefaultRenderContext::new);

    private final DefaultFluidRenderer defaultRenderer;

    public FluidRenderer(ColorProviderRegistry colorProviderRegistry, LightPipelineProvider lighters) {
        defaultRenderer = new DefaultFluidRenderer(colorProviderRegistry, lighters);
    }

    public void render(WorldSlice world, BlockState blockState, FluidState fluidState, BlockPos blockPos, BlockPos offset, ChunkBuildBuffers buffers) {
        var material = DefaultMaterials.forFluidState(fluidState);
        var meshBuilder = buffers.get(material);

        FluidRenderHandler handler = FluidRenderHandlerRegistry.INSTANCE.get(fluidState.getFluid());

        // Match the vanilla FluidRenderer's behavior if the handler is null
        if (handler == null) {
            boolean isLava = fluidState.isIn(FluidTags.LAVA);
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
        defaultContext.setUp(this.defaultRenderer, world, fluidState, blockPos, offset, meshBuilder, material, handler);

        try {
            handler.renderFluid(blockPos, world, meshBuilder.asFallbackVertexConsumer(material), blockState, fluidState);
        } finally {
            defaultContext.clear();
        }
    }

    public static boolean renderFromVanilla() {
        return CURRENT_DEFAULT_CONTEXT.get().renderIfSetUp();
    }

    private static class DefaultRenderContext {
        private DefaultFluidRenderer renderer;
        private WorldSlice world;
        private FluidState fluidState;
        private BlockPos blockPos;
        private BlockPos offset;
        private ChunkModelBuilder meshBuilder;
        private Material material;
        private FluidRenderHandler handler;

        public void setUp(DefaultFluidRenderer renderer, WorldSlice world, FluidState fluidState, BlockPos blockPos, BlockPos offset, ChunkModelBuilder meshBuilder, Material material, FluidRenderHandler handler) {
            this.renderer = renderer;
            this.world = world;
            this.fluidState = fluidState;
            this.blockPos = blockPos;
            this.offset = offset;
            this.meshBuilder = meshBuilder;
            this.material = material;
            this.handler = handler;
        }

        public void clear() {
            this.renderer = null;
            this.world = null;
            this.fluidState = null;
            this.blockPos = null;
            this.offset = null;
            this.meshBuilder = null;
            this.material = null;
            this.handler = null;
        }

        public boolean renderIfSetUp() {
            if (this.renderer != null) {
                this.renderer.render(this.world, this.fluidState, this.blockPos, this.offset, this.meshBuilder, this.material, this.handler);
                return true;
            }

            return false;
        }
    }
}
