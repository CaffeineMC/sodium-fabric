package net.caffeinemc.mods.sodium.client.platform.windows;

import java.util.regex.Pattern;

public record WindowsDriverStoreVersion(int driverModel, int featureLevel, int major, int minor) {
    private static final Pattern PATTERN = Pattern.compile(
            "^(?<driverModel>[0-9]{1,2})\\.(?<featureLevel>[0-9]{1,2})\\.(?<major>[0-9]{1,5})\\.(?<minor>[0-9]{1,5})$");

    public static WindowsDriverStoreVersion parse(String version) throws ParseException {
        var matcher = PATTERN.matcher(version);

        if (!matcher.matches()) {
            throw new ParseException(version);
        }

        var driverModel = Integer.parseInt(matcher.group("driverModel"));
        var featureLevel = Integer.parseInt(matcher.group("featureLevel"));
        var major = Integer.parseInt(matcher.group("major"));
        var minor = Integer.parseInt(matcher.group("minor"));

        return new WindowsDriverStoreVersion(driverModel, featureLevel, major, minor);
    }

    public String getFriendlyString() {
        return "%s.%s.%s.%s".formatted(this.driverModel, this.featureLevel, this.major, this.minor);
    }

    public static class ParseException extends Exception {
        private ParseException(String version) {
            super("Not a valid driver store version (%s)".formatted(version));
        }
    }
}
