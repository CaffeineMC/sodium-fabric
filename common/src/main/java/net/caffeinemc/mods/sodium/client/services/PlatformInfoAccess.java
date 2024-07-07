package net.caffeinemc.mods.sodium.client.services;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface PlatformInfoAccess {
    PlatformInfoAccess INSTANCE = Services.load(PlatformInfoAccess.class);

    static PlatformInfoAccess getInstance() {
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
     * Returns if the FREX Flawless Frames API has been requested by a mod.
     */
    boolean isFlawlessFramesActive();

    /**
     * Returns if the platform has a early loading screen.
     */
    boolean platformHasEarlyLoadingScreen();
}
