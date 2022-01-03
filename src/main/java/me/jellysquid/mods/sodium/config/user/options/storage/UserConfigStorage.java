package me.jellysquid.mods.sodium.config.user.options.storage;

import me.jellysquid.mods.sodium.SodiumClientMod;
import me.jellysquid.mods.sodium.config.user.UserConfig;

import java.io.IOException;

public class UserConfigStorage implements OptionStorage<UserConfig> {
    private final UserConfig options;

    public UserConfigStorage() {
        this.options = SodiumClientMod.options();
    }

    @Override
    public UserConfig getData() {
        return this.options;
    }

    @Override
    public void save() {
        try {
            this.options.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't save configuration changes", e);
        }

        SodiumClientMod.logger().info("Flushed changes to Sodium configuration");
    }
}
