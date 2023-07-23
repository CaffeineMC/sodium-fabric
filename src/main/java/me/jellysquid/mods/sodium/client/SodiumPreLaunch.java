package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.util.workarounds.Workarounds;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class SodiumPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        Workarounds.init();
    }
}
