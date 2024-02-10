package net.caffeinemc.mods.sodium.client;

import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptions;
import net.caffeinemc.mods.sodium.client.data.fingerprint.FingerprintMeasure;
import net.caffeinemc.mods.sodium.client.data.fingerprint.HashedFingerprint;
import net.caffeinemc.mods.sodium.client.gui.console.Console;
import net.caffeinemc.mods.sodium.client.gui.console.message.MessageLevel;
import net.caffeinemc.mods.sodium.client.util.FlawlessFrames;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SodiumClientMod implements ClientModInitializer {
    private static SodiumGameOptions CONFIG;

    @Deprecated
    private static Logger LOGGER;

    private static String MOD_VERSION;

    @Override
    public void onInitializeClient() {
        ModContainer mod = FabricLoader.getInstance()
                .getModContainer("sodium")
                .orElseThrow(NullPointerException::new);

        MOD_VERSION = mod.getMetadata()
                .getVersion()
                .getFriendlyString();

        LOGGER = LoggerFactory.getLogger("Sodium");
        CONFIG = loadConfig();

        FlawlessFrames.onClientInitialization();

        try {
            updateFingerprint();
        } catch (Throwable t) {
            LOGGER.error("Failed to update fingerprint", t);
        }
    }

    /**
     * @return The global configuration for rendering and other functionality
     * @throws IllegalStateException If the configuration has not yet been initialized
     */
    public static SodiumGameOptions options() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }

        return CONFIG;
    }

    /**
     * NOTE: This should not be used by new code. Each caller should instead create their own {@link Logger} and store
     * it within a static field. Doing so provides better context in log files.
     *
     * @return The global logger for rendering and other functionality
     * @throws IllegalStateException If the logger has not yet been initialized
     */
    @Deprecated
    public static Logger logger() {
        if (LOGGER == null) {
            throw new IllegalStateException("Logger not yet available");
        }

        return LOGGER;
    }

    private static SodiumGameOptions loadConfig() {
        try {
            return SodiumGameOptions.loadFromDisk();
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration file", e);
            LOGGER.error("Using default configuration file in read-only mode");

            Console.instance().logMessage(MessageLevel.SEVERE, Component.translatable("sodium.console.config_not_loaded"), 12.5);

            var config = SodiumGameOptions.defaults();
            config.setReadOnly();

            return config;
        }
    }

    @Deprecated
    public static void restoreDefaultOptions() {
        CONFIG = SodiumGameOptions.defaults();

        try {
            SodiumGameOptions.writeToDisk(CONFIG);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config file", e);
        }
    }


    /**
     * @return The version string of the {@link ModContainer}, as provided by the mod loader
     * @throws IllegalStateException If the mod has not yet been initialized
     */
    public static String getVersion() {
        if (MOD_VERSION == null) {
            throw new NullPointerException("Mod version hasn't been populated yet");
        }

        return MOD_VERSION;
    }

    /**
     * Re-calculates the fingerprint, and compares it to the measure hashes on disk. If the fingerprint has changed,
     * some settings will be reset in the configuration file.
     */
    private static void updateFingerprint() {
        var current = FingerprintMeasure.create();

        if (current == null) {
            return;
        }

        HashedFingerprint saved = null;

        try {
            saved = HashedFingerprint.loadFromDisk();
        } catch (Throwable t) {
            LOGGER.error("Failed to load existing fingerprint",  t);
        }

        if (saved == null || !current.looselyMatches(saved)) {
            HashedFingerprint.writeToDisk(current.hashed());

            CONFIG.notifications.hasSeenDonationPrompt = false;
            CONFIG.notifications.hasClearedDonationButton = false;

            try {
                SodiumGameOptions.writeToDisk(CONFIG);
            } catch (IOException e) {
                LOGGER.error("Failed to update config file", e);
            }
        }
    }
}
