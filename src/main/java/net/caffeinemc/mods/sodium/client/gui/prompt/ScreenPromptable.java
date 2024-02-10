package net.caffeinemc.mods.sodium.client.gui.prompt;

import net.caffeinemc.mods.sodium.client.util.Dim2i;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link net.minecraft.client.gui.screens.Screen} implementation which can render "prompts" inside of it.
 */
public interface ScreenPromptable {
    /**
     * Updates the screen to display the given prompt. If an existing prompt is already being displayed, it is instead
     * replaced. If {@param prompt} is null, the current prompt will be closed instead.
     *
     * @param prompt The prompt to display (or null if being removed)
     */
    void setPrompt(@Nullable ScreenPrompt prompt);

    /**
     * @return The prompt being displayed by the screen, if any
     */
    @Nullable ScreenPrompt getPrompt();

    /**
     * @return The dimensions of the screen, used by the prompt for layout
     */
    Dim2i getDimensions();
}
