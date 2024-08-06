package net.caffeinemc.mods.sodium.client.services;

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

    /**
     * Returns if the platform uses refmaps.
     */
    boolean platformUsesRefmap();

    /**
     * Returns if a mod is in the mods folder during loading.
     */
    boolean isModInLoadingList(String modId);
}
