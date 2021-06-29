package me.jellysquid.mods.sodium.client.compat;

import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import org.jetbrains.annotations.NotNull;

public interface FluidRenderHandlerProvider {
    @NotNull FluidRenderHandler get(@NotNull Fluid fluid);
    void onResourceReload(Sprite[] waterSprites, Sprite[] lavaSprites);
}
