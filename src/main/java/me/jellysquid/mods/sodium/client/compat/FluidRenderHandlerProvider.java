package me.jellysquid.mods.sodium.client.compat;

import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
interface FluidRenderHandlerProvider {
    @NotNull FluidRenderHandler get(@NotNull Fluid fluid);

    default void onResourceReload(Sprite[] waterSprites, Sprite[] lavaSprites) { }
}
