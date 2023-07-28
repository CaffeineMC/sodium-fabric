package me.jellysquid.mods.sodium.client.gui.console;

import net.minecraft.client.gui.DrawContext;

public class ConsoleHooks {
    public static void render(DrawContext drawContext, double currentTime) {
        ConsoleRenderer.INSTANCE.update(Console.INSTANCE, currentTime);
        ConsoleRenderer.INSTANCE.draw(drawContext);
    }
}
