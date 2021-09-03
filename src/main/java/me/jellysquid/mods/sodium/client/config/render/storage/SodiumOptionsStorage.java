package me.jellysquid.mods.sodium.client.config.render.storage;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.config.SodiumRenderConfig;

import java.io.IOException;

public class SodiumOptionsStorage implements OptionStorage<SodiumRenderConfig> {
    private final SodiumRenderConfig options;

    public SodiumOptionsStorage() {
        this.options = SodiumClientMod.options();
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

        SodiumClientMod.logger().info("Flushed changes to Sodium configuration");
    }
}
