package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.compatibility.checks.EarlyDriverScanner;
import me.jellysquid.mods.sodium.client.compatibility.workarounds.Workarounds;
import me.jellysquid.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class SodiumPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        GraphicsAdapterProbe.findAdapters();
        EarlyDriverScanner.scanDrivers();
        Workarounds.init();
    }
}
