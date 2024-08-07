package net.caffeinemc.mods.sodium.client.services;

/**
 * Template class for the platform's model data. This is used to pass around Forge model data in a multiloader environment seamlessly.
 */
public interface SodiumModelData {
    SodiumModelData EMPTY = PlatformModelAccess.getInstance().getEmptyModelData();
}
