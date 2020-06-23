package me.jellysquid.mods.sodium.client;

import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;
import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI;

public class ModMenuApiImpl implements ModMenuApi {
    // NOTE for whoever's updating this to 1.16 - this method was deprecated in the snapshots due to advancements in
    // how Fabric handles entrypoints -ADCL
    @Override
    public String getModId() {
        return "sodium"; // I don't see a constant for this anywhere, bad Jelly >:( -ADCL
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SodiumOptionsGUI::new;
    }
}
