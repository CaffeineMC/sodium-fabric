package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.util.workarounds.PrelaunchChecks;
import me.jellysquid.mods.sodium.client.util.workarounds.Workarounds;
import me.jellysquid.mods.sodium.client.util.workarounds.probe.GraphicsAdapterProbe;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public class SodiumPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        var adapters = GraphicsAdapterProbe.findAdapters();

        PrelaunchChecks.checkPlatform();
        PrelaunchChecks.checkDrivers(adapters);

        Workarounds.init(adapters);
    }
}
