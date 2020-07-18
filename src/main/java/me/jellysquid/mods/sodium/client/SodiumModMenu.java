package me.jellysquid.mods.sodium.client;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;

/**
 * Represents the ModMenu integration.
 */
public class SodiumModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SodiumOptionsGUI::new;
    }
}
