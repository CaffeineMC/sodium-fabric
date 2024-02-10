package net.caffeinemc.mods.sodium.client.gui.console.message;

import net.minecraft.network.chat.Component;

public record Message(
        /* The logging level for the message, which can affect the rendering. */
        MessageLevel level,
        /* The text of the message. */
        Component text,
        /* The duration (in seconds) which the message should be shown for. */
        double duration) {

}
