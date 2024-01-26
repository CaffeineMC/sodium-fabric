package me.jellysquid.mods.sodium.client.gui.prompt;

import me.jellysquid.mods.sodium.client.util.Dim2i;
import org.jetbrains.annotations.Nullable;

public interface ScreenPromptable {
    void setPrompt(@Nullable ScreenPrompt prompt);

    @Nullable ScreenPrompt getPrompt();

    Dim2i getDimensions();
}
