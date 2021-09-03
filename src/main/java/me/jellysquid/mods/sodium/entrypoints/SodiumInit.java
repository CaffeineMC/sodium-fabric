package me.jellysquid.mods.sodium.entrypoints;

import me.jellysquid.mods.sodium.SodiumClient;
import net.fabricmc.api.ClientModInitializer;

public class SodiumInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SodiumClient.init();
    }
}
