package me.jellysquid.mods.sodium.common.util;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PathUtil {
    public static Path resolveTimestampedSibling(Path base, String tag) {
        LocalDateTime now = LocalDateTime.now();
        String backupName = base.getFileName().toString();
        backupName = backupName.substring(0, backupName.lastIndexOf('.') - 1);
        backupName += "_" + tag + "_" + now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ".json";
        return base.resolveSibling(backupName);
    }
}
