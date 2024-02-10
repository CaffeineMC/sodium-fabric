package net.caffeinemc.mods.sodium.client.world;

import net.minecraft.client.multiplayer.ClientLevel;

/**
 * An accessor for obtaining the biome seeds from a {@link ClientLevel}.
 */
public interface BiomeSeedProvider {
    /**
     * @see #getBiomeZoomSeed(ClientLevel)
     */
    static long getBiomeZoomSeed(ClientLevel level) {
        return ((BiomeSeedProvider) level).sodium$getBiomeZoomSeed();
    }

    /**
     * @return The seed value used for randomly perturbing the center point of each biome cell in a
     * delaunay triangulation
     */
    long sodium$getBiomeZoomSeed();
}
