package me.jellysquid.mods.sodium.config.render.storage;

import me.jellysquid.mods.sodium.SodiumClient;
import me.jellysquid.mods.sodium.config.SodiumRenderConfig;

import java.io.IOException;

public class SodiumOptionsStorage implements OptionStorage<SodiumRenderConfig> {
    private final SodiumRenderConfig options;

    public SodiumOptionsStorage() {
        this.options = SodiumClient.options();
    }

    @Override
    public SodiumRenderConfig getData() {
        return this.options;
    }

    @Override
    public void save() {
        try {
            this.options.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't save configuration changes", e);
        }

        SodiumClient.logger().info("Flushed changes to Sodium configuration");
    }
}
