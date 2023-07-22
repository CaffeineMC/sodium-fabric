package me.jellysquid.mods.sodium.client.util.workarounds;

import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Workarounds {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium");

    private static final AtomicReference<Set<Reference>> ACTIVE_WORKAROUNDS = new AtomicReference<>(EnumSet.noneOf(Reference.class));

    public static void init() {
        var graphicsVendors = GraphicsAdapterProbe.findAdapters();
        var workarounds = findNecessaryWorkarounds(graphicsVendors);

        if (!workarounds.isEmpty()) {
            LOGGER.warn("One or more workarounds were enabled to prevent crashes or other issues on your system: [{}]", workarounds.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(", ")));
            LOGGER.warn("This might indicate that you need to update your graphics drivers.");
        }

        ACTIVE_WORKAROUNDS.set(workarounds);
    }

    private static Set<Reference> findNecessaryWorkarounds(List<GraphicsAdapterProbe.Result> graphicsAdapters) {
        var workarounds = EnumSet.noneOf(Reference.class);
        var operatingSystem = Util.getOperatingSystem();

        if (operatingSystem == Util.OperatingSystem.WINDOWS && graphicsAdapters.stream()
                .anyMatch(adapter -> adapter.vendor() == GraphicsAdapterProbe.Vendor.NVIDIA)) {
            workarounds.add(Reference.NVIDIA_BAD_DRIVER_SETTINGS);
        }

        if (operatingSystem == Util.OperatingSystem.LINUX && graphicsAdapters.stream()
                .anyMatch(adapter -> adapter.vendor() == GraphicsAdapterProbe.Vendor.NVIDIA)) {
            workarounds.add(Reference.NVIDIA_BAD_DRIVER_LINUX);
        }

        return Collections.unmodifiableSet(workarounds);
    }

    public static boolean isWorkaroundEnabled(Reference id) {
        return ACTIVE_WORKAROUNDS.get()
                .contains(id);
    }

    public enum Reference {
        NVIDIA_BAD_DRIVER_SETTINGS,
        NVIDIA_BAD_DRIVER_LINUX
    }
}
