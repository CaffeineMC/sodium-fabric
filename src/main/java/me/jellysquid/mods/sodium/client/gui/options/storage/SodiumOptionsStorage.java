package me.jellysquid.mods.sodium.client.gui.options.storage;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.data.config.UserConfig;

import java.io.IOException;

public class SodiumOptionsStorage implements OptionStorage<UserConfig> {
    private final UserConfig options;

    public SodiumOptionsStorage() {
        this.options = SodiumClientMod.options();
    }

    @Override
    public UserConfig getData() {
        return this.options;
    }

    @Override
    public void save() {
        try {
            UserConfig.writeToDisk(this.options);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't save configuration changes", e);
        }

        SodiumClientMod.logger().info("Flushed changes to Sodium configuration");
    }
}
