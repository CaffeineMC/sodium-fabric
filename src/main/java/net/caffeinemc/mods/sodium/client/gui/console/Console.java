package net.caffeinemc.mods.sodium.client.gui.console;

import net.caffeinemc.mods.sodium.client.gui.console.message.Message;
import net.caffeinemc.mods.sodium.client.gui.console.message.MessageLevel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * The default implementation of the console, which collects messages into a deque. This is done because the renderer
 * may not immediately be available while messages are being generated.
 */
public class Console implements ConsoleSink {
    static final Console INSTANCE = new Console();

    private final ArrayDeque<Message> messages = new ArrayDeque<>();

    @Override
    public void logMessage(@NotNull MessageLevel level, @NotNull Component text, double duration) {
        Objects.requireNonNull(level);
        Objects.requireNonNull(text);

        this.messages.addLast(new Message(level, text.copy(), duration));
    }

    /**
     * Returns a {@link Deque} over the messages which have not yet been consumed. The rendering implementation should
     * call this periodically to remove any new messages from the queue.
     * @return A deque over the currently pending messages
     */
    public Deque<Message> getMessageDrain() {
        return this.messages;
    }

    /**
     * @return The global instance which other callers can use to log messages
     */
    public static ConsoleSink instance() {
        return INSTANCE;
    }
}
