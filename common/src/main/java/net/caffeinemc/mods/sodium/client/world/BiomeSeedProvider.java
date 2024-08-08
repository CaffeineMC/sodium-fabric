package net.caffeinemc.mods.sodium.client.world;

import net.minecraft.client.multiplayer.ClientLevel;

public interface BiomeSeedProvider {
    static long getBiomeZoomSeed(ClientLevel level) {
        return ((BiomeSeedProvider) level).sodium$getBiomeZoomSeed();
    }

    long sodium$getBiomeZoomSeed();
}
