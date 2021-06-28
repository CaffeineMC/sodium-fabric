package me.jellysquid.mods.sodium.client.compat;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CompatHolder {
    private CompatHolder() { }

    public static void init() { }

    public static @NotNull WorldSlice createWorldSlice(@NotNull World world) {
        return WORLD_SLICE_FACTORY.create(world);
    }

    public static @NotNull FluidRenderHandler getFluidRenderHandler(@NotNull Fluid fluid) {
        return FLUID_RENDER_HANDLER_PROVIDER.get(fluid);
    }

    public static void onFluidResourceReload(Sprite[] waterSprites, Sprite[] lavaSprites) {
        FLUID_RENDER_HANDLER_PROVIDER.onResourceReload(waterSprites, lavaSprites);
    }

    private static boolean isModLoaded(@NotNull String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    private static final WorldSliceFactory WORLD_SLICE_FACTORY = createWorldSliceFactory();
    private static final FluidRenderHandlerProvider FLUID_RENDER_HANDLER_PROVIDER = createFluidRendererOverrideProvider();

    private static @NotNull WorldSliceFactory createWorldSliceFactory() {
        if (isModLoaded("fabric-rendering-data-attachment-v1"))
            return RenderAttachedWorldSlice::new;
        else
            return WorldSlice::new;
    }

    private static @NotNull FluidRenderHandlerProvider createFluidRendererOverrideProvider() {
        if (isModLoaded("fabric-rendering-fluids-v1"))
            return new FluidRenderHandlerProvider() {
                private final Object2ReferenceOpenHashMap<Fluid, FluidRenderHandler> cache
                        = new Object2ReferenceOpenHashMap<>();

                @Override
                public @NotNull FluidRenderHandler get(@NotNull Fluid fluid) {
                    return cache.computeIfAbsent(fluid, fluid1 -> {
                        net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler delegate
                                = FluidRenderHandlerRegistry.INSTANCE.get(fluid);
                        return new FluidRenderHandler() {
                            @Override
                            public Sprite[] getFluidSprites(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {
                                return delegate.getFluidSprites(view, pos, state);
                            }

                            @Override
                            public int getFluidColor(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {
                                return delegate.getFluidColor(view, pos, state);
                            }
                        };
                    });
                }
            };
        else
            return new VanillaFluidRenderHandlerProvider();
    }
}
