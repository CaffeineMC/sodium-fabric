package me.jellysquid.mods.sodium.client.gui.console;

import net.minecraft.client.gui.GuiGraphics;

public class ConsoleHooks {
    public static void render(GuiGraphics drawContext, double currentTime) {
        ConsoleRenderer.INSTANCE.update(Console.INSTANCE, currentTime);
        ConsoleRenderer.INSTANCE.draw(drawContext);
    }
}
