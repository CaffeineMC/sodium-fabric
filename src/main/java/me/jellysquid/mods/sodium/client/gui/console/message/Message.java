package me.jellysquid.mods.sodium.client.gui.console.message;

import net.minecraft.network.chat.Component;

public record Message(MessageLevel level, Component text, double duration) {

}
