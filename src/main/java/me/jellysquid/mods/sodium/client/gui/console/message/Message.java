package me.jellysquid.mods.sodium.client.gui.console.message;

import net.minecraft.text.Text;

public record Message(MessageLevel level, Text text, double duration) {

}
