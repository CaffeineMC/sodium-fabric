package me.jellysquid.mods.sodium.client.util.workarounds.driver.nvidia;

import me.jellysquid.mods.sodium.client.util.workarounds.platform.windows.WindowsDriverStoreVersion;

public record NvidiaDriverVersion(int major, int minor) {
    public static NvidiaDriverVersion parse(WindowsDriverStoreVersion version) throws ParseException {
        var buffer = new StringBuilder(32);
        buffer.append(version.major());
        buffer.append(version.minor());

        var string = buffer.toString();

        if (string.length() < 3 || string.length() > 6) {
            throw new ParseException(version);
        }

        // TODO: This does not work for driver versions <100.00 or >999.99
        // Really, in general, this code is terrible. The versions which NVIDIA uses for the driver store
        // are not reasonable.
        int major, minor;

        try {
            major = Integer.parseInt(string.substring(string.length() - 5, string.length() - 2));
            minor = Integer.parseInt(string.substring(string.length() - 2));
        } catch (NumberFormatException e) {
            throw new ParseException(version, e);
        }

        return new NvidiaDriverVersion(major, minor);
    }

    /**
     * @param oldest The oldest version (inclusive) to test against
     * @param newest The newest version (exclusive) to test against
     * @return True if this version is within the specified version range
     */
    public boolean isWithinRange(NvidiaDriverVersion oldest, NvidiaDriverVersion newest) {
        // Fail when (this < oldest)
        if (this.major < oldest.major || this.minor < oldest.minor) {
            return false;
        }

        // Fail when (this >= newest)
        if (this.major >= newest.major && this.minor >= newest.minor) {
            return false;
        }

        // Succeed when (this >= oldest) and (this < newest)
        return true;
    }

    public static class ParseException extends Exception {
        private ParseException(WindowsDriverStoreVersion version) {
            super(getMessage(version));
        }

        private ParseException(WindowsDriverStoreVersion version, Exception inner) {
            super(getMessage(version), inner);
        }

        private static String getMessage(WindowsDriverStoreVersion version) {
            return "Not a valid NVIDIA driver version (%s)".formatted(version);
        }
    }
}
