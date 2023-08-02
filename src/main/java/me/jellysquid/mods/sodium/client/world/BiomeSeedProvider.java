package me.jellysquid.mods.sodium.client.world;

import net.minecraft.client.world.ClientWorld;

public interface BiomeSeedProvider {
    static long getBiomeSeed(ClientWorld world) {
        return ((BiomeSeedProvider) world).sodium$getBiomeSeed();
    }

    long sodium$getBiomeSeed();
}
