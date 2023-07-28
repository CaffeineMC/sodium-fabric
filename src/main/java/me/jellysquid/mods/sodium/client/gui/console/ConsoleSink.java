package me.jellysquid.mods.sodium.client.gui.console;

import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public interface ConsoleSink {
    void logMessage(@NotNull MessageLevel level, @NotNull Text text, double duration);
}
