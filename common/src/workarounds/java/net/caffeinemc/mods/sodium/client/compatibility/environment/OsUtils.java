package net.caffeinemc.mods.sodium.client.compatibility.environment;

import org.apache.commons.lang3.SystemUtils;

public class OsUtils {

    public static OperatingSystem getOs() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return OperatingSystem.WIN;
        } else if (SystemUtils.IS_OS_MAC) {
            return OperatingSystem.MAC;
        } else if (SystemUtils.IS_OS_LINUX) {
            return OperatingSystem.LINUX;
        }

        return OperatingSystem.UNKNOWN;
    }

    public enum OperatingSystem {
        WIN,
        MAC,
        LINUX,
        UNKNOWN
    }
}