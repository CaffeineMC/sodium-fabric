package me.jellysquid.mods.sodium.client;

import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.opengl.GL11;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class SodiumHooks {
    public static BooleanSupplier useClipping = () -> false;

    public static BooleanSupplier shouldEnableClipping = () -> false;

    public static Supplier<float[]> getClippingEquation = () -> {
        return null;
    };
}
