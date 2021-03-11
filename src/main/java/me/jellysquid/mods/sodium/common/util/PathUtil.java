package me.jellysquid.mods.sodium.common.util;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import static java.time.temporal.ChronoField.*;

public class PathUtil {
    /**
     * A variant of {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME} that is safe for file names (IE doesn't contain colons).
     */
    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME_SAFE = new DateTimeFormatterBuilder()
            .parseCaseInsensitive().parseStrict()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral('-')
            .appendValue(MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral('-')
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendFraction(NANO_OF_SECOND, 0, 9, true)
            .toFormatter(Locale.ROOT);

    /**
     * Resolves a timestamped sibling of the base path - {@code "{base_dir}/{base_name}_{tag}_{timestamp}.{base_ext}"}.
     * @param base Base path
     * @param tag Tag to add to sibling's name
     * @return Sibling path
     */
    public static Path resolveTimestampedSibling(Path base, String tag) {
        LocalDateTime now = LocalDateTime.now();
        String backupName = base.getFileName().toString();
        backupName = backupName.substring(0, backupName.lastIndexOf('.') - 1);
        backupName += "_" + tag + "_" + now.format(ISO_LOCAL_DATE_TIME_SAFE) + ".json";
        return base.resolveSibling(backupName);
    }
}
