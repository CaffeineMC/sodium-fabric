package me.jellysquid.mods.sodium.interop.vanilla.world;

public interface ClientWorldExtended {
    /**
     * @return The world seed used for generating biome data on the client
     */
    long getBiomeSeed();
}
