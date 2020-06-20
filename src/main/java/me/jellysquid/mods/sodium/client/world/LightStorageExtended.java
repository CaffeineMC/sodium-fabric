package me.jellysquid.mods.sodium.client.world;

public interface LightStorageExtended {
    void runPendingUpdates();

    void removeColumnWithoutUpdate(long columnPos);
}
