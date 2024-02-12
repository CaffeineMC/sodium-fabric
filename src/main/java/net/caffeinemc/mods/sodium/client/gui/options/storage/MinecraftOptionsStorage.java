package net.caffeinemc.mods.sodium.client.gui.options.storage;

import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

public class MinecraftOptionsStorage implements OptionStorage<Options> {
    private final Minecraft client;

    public MinecraftOptionsStorage() {
        this.client = Minecraft.getInstance();
    }

    @Override
    public Options getData() {
        return this.client.options;
    }

    @Override
    public void save() {
        this.getData().save();

        SodiumClientMod.logger().info("Flushed changes to Minecraft configuration");
    }
}
