package net.caffeinemc.mods.sodium.client.services;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;

import java.nio.file.Path;

public interface PlatformRuntimeInformation {
    PlatformRuntimeInformation INSTANCE = Services.load(PlatformRuntimeInformation.class);

    static PlatformRuntimeInformation getInstance() {
        return INSTANCE;
    }

    /**
     * Returns if the user is running in a development environment.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Returns the current game directory the user is running in.
     */
    Path getGameDirectory();

    /**
     * Returns the current configuration directory for the platform.
     */
    Path getConfigDirectory();

    /**
     * Returns if the platform has a early loading screen.
     */
    boolean platformHasEarlyLoadingScreen();

    /**
     * Returns if the platform uses refmaps.
     */
    boolean platformUsesRefmap();

    /**
     * Returns if a mod is in the mods folder during loading.
     */
    boolean isModInLoadingList(String modId);

    /**
     * @return Whether {@link VertexConsumer#putBulkData(PoseStack.Pose, BakedQuad, float[], float, float, float, float, int[], int, boolean)} should multiply the vertex alpha component.
     */
    boolean usesAlphaMultiplication();
}
