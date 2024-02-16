package net.caffeinemc.mods.sodium.client.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileUtil {
    public static void writeTextRobustly(String text, Path path) throws IOException {
        // Use a temporary location next to the config's final destination
        Path tempPath = path.resolveSibling(path.getFileName() + ".tmp");

        // Write the file to our temporary location
        Files.writeString(tempPath, text);

        // Atomically replace the old config file (if it exists) with the temporary file
        Files.move(tempPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
