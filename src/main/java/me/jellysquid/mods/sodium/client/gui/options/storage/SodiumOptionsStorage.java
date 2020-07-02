package me.jellysquid.mods.sodium.client.gui.options.storage;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;

public class SodiumOptionsStorage implements OptionStorage<SodiumGameOptions> {
    private final SodiumGameOptions options;

    public SodiumOptionsStorage() {
        this.options = SodiumClientMod.options();
    }

    @Override
    public SodiumGameOptions getData() {
        return this.options;
    }

    @Override
    public void save() {
        SodiumClientMod.onConfigChanged(this.options);

        this.options.writeChanges();

        SodiumClientMod.logger().info("Flushed changes to Sodium configuration");
    }
}
