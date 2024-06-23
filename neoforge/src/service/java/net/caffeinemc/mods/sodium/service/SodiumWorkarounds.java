package net.caffeinemc.mods.sodium.service;

import net.caffeinemc.mods.sodium.client.compatibility.checks.PreLaunchChecks;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.caffeinemc.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaWorkarounds;
import net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper;

public class SodiumWorkarounds implements GraphicsBootstrapper {
    @Override
    public String name() {
        return "sodium";
    }

    @Override
    public void bootstrap(String[] arguments) {
        PreLaunchChecks.beforeLWJGLInit();
        GraphicsAdapterProbe.findAdapters();
        PreLaunchChecks.onGameInit();
        Workarounds.init();
        final boolean applyNvidiaWorkarounds = Workarounds.isWorkaroundEnabled(Workarounds.Reference.NVIDIA_THREADED_OPTIMIZATIONS);

        if (applyNvidiaWorkarounds) {
            System.out.println("[Sodium] Applying NVIDIA workarounds earlier on Forge.");
            NvidiaWorkarounds.install();
        }
    }
}
