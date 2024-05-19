package net.caffeinemc.mods.sodium.client.compatibility.environment.probe;

import org.jetbrains.annotations.NotNull;

public interface GraphicsAdapterInfo {
    @NotNull GraphicsAdapterVendor vendor();

    @NotNull String name();

    record LinuxPciAdapterInfo(
            @NotNull GraphicsAdapterVendor vendor,
            @NotNull String name,

            String pciVendorId,
            String pciDeviceId
    ) implements GraphicsAdapterInfo {

    }
}