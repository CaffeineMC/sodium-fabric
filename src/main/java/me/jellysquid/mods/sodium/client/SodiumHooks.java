package me.jellysquid.mods.sodium.client;

import net.fabricmc.loader.api.FabricLoader;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class SodiumHooks {
    public static boolean useClipping() {
        return FabricLoader.getInstance().isModLoaded("imm_ptl_core");
    }

    public static BooleanSupplier shouldEnableClipping = () -> false;

    public static Supplier<float[]> getClippingEquation = () -> null;
}
