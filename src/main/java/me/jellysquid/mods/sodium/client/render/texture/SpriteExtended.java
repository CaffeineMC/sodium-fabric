package me.jellysquid.mods.sodium.client.render.texture;

public interface SpriteExtended {
    void uploadPendingChanges();

    void markActive();
}
