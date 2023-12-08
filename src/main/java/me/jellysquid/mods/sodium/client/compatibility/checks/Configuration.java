package me.jellysquid.mods.sodium.client.compatibility.checks;

/**
 * "Checks" are used to determine whether the environment we are running within is actually reasonable. Most often,
 * failing checks will crash the game and prompt the user for intervention.
 */
class Configuration {
    public static final boolean WIN32_RTSS_HOOKS = configureCheck("win32.rtss", true);
    public static final boolean WIN32_DRIVER_INTEL_GEN7 = configureCheck("win32.intelGen7", true);

    private static boolean configureCheck(String name, boolean defaultValue) {
        var propertyValue = System.getProperty(getPropertyKey(name), null);

        if (propertyValue == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(propertyValue);
    }

    private static String getPropertyKey(String name) {
        return "sodium.checks." + name;
    }
}
