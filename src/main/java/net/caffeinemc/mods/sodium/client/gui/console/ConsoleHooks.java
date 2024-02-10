package net.caffeinemc.mods.sodium.client.gui.console;

import net.minecraft.client.gui.GuiGraphics;

public class ConsoleHooks {
    /**
     * Called each frame to render the console to the screen.
     * @param graphics The GUI graphics context
     * @param currentTime The timestamp (in seconds) of the frame being rendered
     */
    public static void render(GuiGraphics graphics, double currentTime) {
        ConsoleRenderer.INSTANCE.update(Console.INSTANCE, currentTime);
        ConsoleRenderer.INSTANCE.draw(graphics, currentTime);
    }
}
