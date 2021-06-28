package me.jellysquid.mods.sodium.client.compat;

import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public final class CompatHolder {
    private CompatHolder() { }

    public static void init() { }

    public static @NotNull WorldSlice createWorldSlice(@NotNull World world) {
        return WORLD_SLICE_FACTORY.create(world);
    }

    private static boolean isModLoaded(@NotNull String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    private static final WorldSliceFactory WORLD_SLICE_FACTORY = createWorldSliceFactory();

    private static @NotNull WorldSliceFactory createWorldSliceFactory() {
        if (isModLoaded("fabric-rendering-data-attachment-v1"))
            return RenderAttachedWorldSlice::new;
        else
            return WorldSlice::new;
    }
}
