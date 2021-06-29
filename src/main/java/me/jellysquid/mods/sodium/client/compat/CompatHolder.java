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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds compatibility-related class/interface instances.
 */
@ApiStatus.Internal
public final class CompatHolder {
    private CompatHolder() { }

    public static void init() { /* <clinit> */ }

    private static boolean isModLoaded(@NotNull String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static final WorldSliceFactory WORLD_SLICE_FACTORY = createWorldSliceFactory();
    public static final FluidRenderHandlerProvider FLUID_RENDER_HANDLER_PROVIDER = createFluidRenderHandlerProvider();

    private static @NotNull WorldSliceFactory createWorldSliceFactory() {
        if (isModLoaded("fabric-rendering-data-attachment-v1"))
            return RenderAttachedWorldSlice::new;
        else
            return WorldSlice::new;
    }

    private static @NotNull FluidRenderHandlerProvider createFluidRenderHandlerProvider() {
        if (isModLoaded("fabric-rendering-fluids-v1"))
            return new FluidRenderHandlerProvider() {
                private final Object2ReferenceOpenHashMap<Fluid, FluidRenderHandler> cache
                        = new Object2ReferenceOpenHashMap<>();

                @Override
                public @NotNull FluidRenderHandler get(@NotNull Fluid fluid) {
                    return cache.computeIfAbsent(fluid, fluid1 -> {
                        net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler delegate
                                = FluidRenderHandlerRegistry.INSTANCE.get(fluid1);
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

                @Override
                public void onResourceReload(Sprite[] waterSprites, Sprite[] lavaSprites) {
                    // clear cache, since sprites could have changed
                    cache.clear();
                }
            };
        else
            return new VanillaFluidRenderHandlerProvider();
    }
}
