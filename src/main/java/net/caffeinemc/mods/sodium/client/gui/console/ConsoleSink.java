package net.caffeinemc.mods.sodium.client.gui.console;

import net.caffeinemc.mods.sodium.client.gui.console.message.MessageLevel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * The console system provides a simple interface for showing temporary graphical messages to the user. These are
 * rendered in the form of "toasts" in the top-left corner of the screen, and can be styled using {@link MessageLevel}.
 */
public interface ConsoleSink {
    /**
     * Publishes a message to the console. The implementation will choose how the message is rendered, and
     * provide interactive features for it.
     *
     * @param level The level of the message
     * @param text The text body of the message
     * @param duration The duration (in seconds) which the message should be shown for
     */
    void logMessage(@NotNull MessageLevel level, @NotNull Component text, double duration);
}
