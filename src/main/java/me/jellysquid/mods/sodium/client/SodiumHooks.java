package me.jellysquid.mods.sodium.client;

import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.opengl.GL11;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class SodiumHooks {
    public static boolean useClipping() {
        return true;
//        return FabricLoader.getInstance().isModLoaded("imm_ptl_core");
    }

    public static BooleanSupplier shouldEnableClipping = () -> true;

    public static Supplier<float[]> getClippingEquation = () -> {
        GL11.glEnable(GL11.GL_CLIP_PLANE0);
        return new float[]{1,0,0,0};
    };
}
