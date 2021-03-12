package me.jellysquid.mods.sodium.common.util;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import static java.time.temporal.ChronoField.*;

public class PathUtil {
    /**
     * A variant of {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME} that is:
     * - Safe for file names (IE doesn't contain colons).
     * - More compact (uses {@link DateTimeFormatter#BASIC_ISO_DATE}'s format for date component and a similar format for time component).
     */
    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME_SAFE = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().parseStrict()
            .appendValue(YEAR, 4)
            .appendValue(MONTH_OF_YEAR, 2)
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral('T')
            .appendValue(HOUR_OF_DAY, 2)
            .appendValue(MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendLiteral('N')
            .appendFraction(NANO_OF_SECOND, 0, 9, false)
            .toFormatter(Locale.ROOT);

    /**
     * Resolves a timestamped sibling of the base path - {@code "{base}-{tag}-{timestamp}"}.
     * @param base Base path
     * @param tag Tag to add to sibling's name
     * @return Sibling path
     */
    public static Path resolveTimestampedSibling(Path base, String tag) {
        return base.resolveSibling(base.getFileName().toString()
                + "-" + tag
                + "-" + LocalDateTime.now().format(ISO_LOCAL_DATE_TIME_SAFE));
    }
}
