package me.jellysquid.mods.sodium.config.render.storage;

import me.jellysquid.mods.sodium.SodiumClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

public class MinecraftOptionsStorage implements OptionStorage<GameOptions> {
    private final MinecraftClient client;

    public MinecraftOptionsStorage() {
        this.client = MinecraftClient.getInstance();
    }

    @Override
    public GameOptions getData() {
        return this.client.options;
    }

    @Override
    public void save() {
        this.getData().write();

        SodiumClient.logger().info("Flushed changes to Minecraft configuration");
    }
}
