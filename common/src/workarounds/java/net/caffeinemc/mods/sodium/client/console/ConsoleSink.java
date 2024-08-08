package net.caffeinemc.mods.sodium.client.console;

import net.caffeinemc.mods.sodium.client.console.message.MessageLevel;
import org.jetbrains.annotations.NotNull;

public interface ConsoleSink {
    void logMessage(@NotNull MessageLevel level, @NotNull String text, boolean translatable, double duration);
}
