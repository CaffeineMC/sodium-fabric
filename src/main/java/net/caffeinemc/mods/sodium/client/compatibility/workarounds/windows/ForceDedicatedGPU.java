package net.caffeinemc.mods.sodium.client.compatibility.workarounds.windows;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.WinReg;
import net.caffeinemc.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import net.caffeinemc.mods.sodium.client.platform.windows.api.Kernel32;
import net.caffeinemc.mods.sodium.client.platform.windows.api.d3dkmt.D3DKMT;
import net.caffeinemc.mods.sodium.client.util.OsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class ForceDedicatedGPU {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-ForceDedicatedGPU");
    public static final String USER_GPU_PREFERENCE_REGISTRY = "Software\\Microsoft\\DirectX\\UserGpuPreferences";
    private static final int PREFERENCE_WINDOWS_DECIDE = 0;
    private static final int PREFERENCE_POWER_SAVING = 1;
    private static final int PREFERENCE_HIGH_PERFORMANCE = 2;
    private static final int PREFERENCE_USER_SPECIFIC = 0x40000000;
    public static boolean shouldForceDedicatedGPU(OsUtils.OperatingSystem operatingSystem, int adapterCount) {
        if (adapterCount <= 1) {//Dont do it if there is only 1 (or none) adapters
            return false;
        }
        if (operatingSystem != OsUtils.OperatingSystem.WIN) {
            return false;
        }
        if (!VersionHelpers.IsWindows10OrGreater()) {
            //Only works on windows 10 and up for the time being
            return false;
        }
        var file = Kernel32.getModuleFileName(0);
        try {
            //Check if the registry contains a preference, if so, fetch and check preference
            if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, USER_GPU_PREFERENCE_REGISTRY, file, 0x20019)) {
                var userPreference = Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, USER_GPU_PREFERENCE_REGISTRY, file, 0x20019);
                if (userPreference != null && !userPreference.isEmpty()) {
                    int preference = -1;

                    //Need to check what the user preference is if possible
                    for (var part : userPreference.split(";")) {
                        var pieces = part.split("=");
                        if (pieces[0].equals("GpuPreference") && pieces.length == 2) {
                            try {
                                preference = Integer.parseUnsignedInt(pieces[1]);
                            } catch (NumberFormatException e) {
                                LOGGER.warn("GpuPreference had an unparsable number: " + pieces[1]);
                            }
                            break;
                        }
                    }

                    if (preference == PREFERENCE_POWER_SAVING || preference == PREFERENCE_HIGH_PERFORMANCE || preference == PREFERENCE_USER_SPECIFIC) {
                        LOGGER.info(String.format("User has specified a graphics preference, not forcing gpu: %X", preference));
                        return false;
                    }
                }
            }
            //We have free rein as user has not specified a preference, or the registry is corrupted
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to fetch the user preference from the registry.", e);
            return false;
        }
    }


    public static void forceDedicatedGpu() {
        D3DKMT.WDDMAdapterInfo selected = null;
        for (var adapter : GraphicsAdapterProbe.getAdapters()) {
            if (adapter instanceof D3DKMT.WDDMAdapterInfo wddmAdapterInfo) {
                //Find target gpu, for the time being select the first dgpu
                if ((wddmAdapterInfo.adapterType()&0x10)!=0) {
                    selected = wddmAdapterInfo;
                    break;
                }
            }
        }
        if (selected == null) {
            LOGGER.info("Unable to find a dedicated gpu to launch the game on.");
            return;
        }
        LOGGER.info("Attempting to forcefully set the gpu used to " + selected);

        //Need to force the preference type to be user selection
        for (var adapter : GraphicsAdapterProbe.getAdapters()) {
            if (adapter instanceof D3DKMT.WDDMAdapterInfo wddmAdapterInfo) {
                D3DKMT.d3dkmtCacheHybridQueryValue(wddmAdapterInfo.luid(), 5 /*D3DKMT_GPU_PREFERENCE_STATE_USER_SPECIFIED_GPU*/, false, 2/*D3DKMT_GPU_PREFERENCE_TYPE_USER_PREFERENCE*/);
            }
        }

        //Prime the kernel adapter cache with the selected adapter
        D3DKMT.setPciProperties(1 /*USER_SETTINGS*/, selected.pciInfo());
        D3DKMT.setPciProperties(2 /*GLOBAL_SETTINGS*/, selected.pciInfo());
    }
}
