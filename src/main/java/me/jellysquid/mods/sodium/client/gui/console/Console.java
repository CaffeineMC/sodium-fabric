package me.jellysquid.mods.sodium.client.gui.console;

import me.jellysquid.mods.sodium.client.gui.console.message.Message;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public class Console implements ConsoleSink {
    static final Console INSTANCE = new Console();

    private final ArrayDeque<Message> messages = new ArrayDeque<>();

    @Override
    public void logMessage(@NotNull MessageLevel level, @NotNull Component text, double duration) {
        Objects.requireNonNull(level);
        Objects.requireNonNull(text);

        this.messages.addLast(new Message(level, text.copy(), duration));
    }

    public Deque<Message> getMessageDrain() {
        return this.messages;
    }

    public static ConsoleSink instance() {
        return INSTANCE;
    }
}
