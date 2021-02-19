package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.opengl.GL11;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

// Hooks of Sodium that are used by Immersive Portals
public class SodiumHooks {
    public static BooleanSupplier useClipping = () -> false;

    public static BooleanSupplier shouldEnableClipping = () -> false;

    public static Supplier<float[]> getClippingEquation = () -> {
        return null;
    };

    public static Predicate<ChunkRenderContainer<?>> shouldCull = c -> false;
}
