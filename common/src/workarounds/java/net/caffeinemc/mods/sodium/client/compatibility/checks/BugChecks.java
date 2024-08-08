package net.caffeinemc.mods.sodium.client.compatibility.checks;

/**
 * "Checks" are used to determine whether the environment we are running within is actually reasonable. Most often,
 * failing checks will crash the game and prompt the user for intervention.
 */
class BugChecks {
    public static final boolean ISSUE_899 = configureCheck("issue899", true);
    public static final boolean ISSUE_1486 = configureCheck("issue1486", true);
    public static final boolean ISSUE_2048 = configureCheck("issue2048", true);
    public static final boolean ISSUE_2561 = configureCheck("issue2561", true);
    public static final boolean ISSUE_2637 = configureCheck("issue2637", true);

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
