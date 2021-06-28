package me.jellysquid.mods.sodium.client.compat;

import it.unimi.dsi.fastutil.objects.Object2ReferenceArrayMap;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.BiomeKeys;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

final class VanillaFluidRenderHandlerProvider implements FluidRenderHandlerProvider {
    private static final int DEFAULT_WATER_COLOR
            = Objects.requireNonNull(BuiltinRegistries.BIOME.get(BiomeKeys.OCEAN)).getWaterColor();
    private final Object2ReferenceArrayMap<Fluid, FluidRenderHandler> handlers = new Object2ReferenceArrayMap<>();

    @Override
    public @NotNull FluidRenderHandler get(@NotNull Fluid fluid) {
        FluidRenderHandler handler = handlers.get(fluid);
        if (handler == null)
            throw new RuntimeException("Unhandled fluid " + Registry.FLUID.getKey(fluid) + "!");
        return handler;
    }

    @Override
    public void onResourceReload(Sprite[] waterSprites, Sprite[] lavaSprites) {
        FluidRenderHandler waterHandler = new FluidRenderHandler() {
            @Override
            public Sprite[] getFluidSprites(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {
                return waterSprites;
            }

            @Override
            public int getFluidColor(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {
                if (view == null)
                    return DEFAULT_WATER_COLOR;
                return BiomeColors.getWaterColor(view, pos);
            }
        };
        FluidRenderHandler lavaHandler = new FluidRenderHandler() {
            @Override
            public Sprite[] getFluidSprites(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {
                return lavaSprites;
            }

            @Override
            public int getFluidColor(@Nullable BlockRenderView view, @Nullable BlockPos pos, FluidState state) {
                return -1;
            }
        };

        handlers.put(Fluids.WATER, waterHandler);
        handlers.put(Fluids.FLOWING_WATER, waterHandler);
        handlers.put(Fluids.LAVA, lavaHandler);
        handlers.put(Fluids.FLOWING_LAVA, lavaHandler);
    }
}
