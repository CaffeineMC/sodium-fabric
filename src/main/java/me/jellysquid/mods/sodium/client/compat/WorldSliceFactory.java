package me.jellysquid.mods.sodium.client.compat;

import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
interface WorldSliceFactory {
    @NotNull WorldSlice create(@NotNull World world);
}
