package net.caffeinemc.mods.sodium.client.util.task;

public interface CancellationToken {
    boolean isCancelled();

    void setCancelled();
}
