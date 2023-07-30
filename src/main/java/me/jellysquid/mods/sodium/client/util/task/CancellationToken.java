package me.jellysquid.mods.sodium.client.util.task;

public interface CancellationToken {
    boolean isCancelled();

    void setCancelled();
}
