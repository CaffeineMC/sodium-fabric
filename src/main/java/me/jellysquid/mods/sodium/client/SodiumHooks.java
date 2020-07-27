package me.jellysquid.mods.sodium.client;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class SodiumHooks {
    public static BooleanSupplier shouldEnableCulling = () -> false;

    public static Supplier<float[]> getCullingEquation = () -> null;
}
