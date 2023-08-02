package me.jellysquid.mods.sodium.client.util.workarounds.probe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GraphicsAdapterProbe {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-GraphicsAdapterProbe");

    private static List<GraphicsAdapterInfo> ADAPTERS;

    public static void findAdapters() {
        LOGGER.info("Searching for graphics cards...");

        var systemInfo = new SystemInfo();
        var hardwareInfo = systemInfo.getHardware();

        var results = new ArrayList<GraphicsAdapterInfo>();

        for (var graphicsCard : hardwareInfo.getGraphicsCards()) {
            GraphicsAdapterVendor vendor = GraphicsAdapterVendor.identifyVendorFromString(graphicsCard.getVendor());
            String name = graphicsCard.getName();
            String versionInfo = graphicsCard.getVersionInfo();

            var info = new GraphicsAdapterInfo(vendor, name, versionInfo);
            results.add(info);

            LOGGER.info("Found graphics card: {}", info);
        }

        if (results.isEmpty()) {
            LOGGER.warn("No graphics cards were found. Either you have no hardware devices supporting 3D acceleration, or " +
                    "something has gone terribly wrong!");
        }

        ADAPTERS = results;
    }

    public static Collection<GraphicsAdapterInfo> getAdapters() {
        if (ADAPTERS == null) {
            throw new RuntimeException("Graphics adapters not probed yet");
        }

        return ADAPTERS;
    }
}
