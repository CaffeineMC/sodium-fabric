package me.jellysquid.mods.sodium.client.util.workarounds.classpath;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class LibraryClasspaths {
    public static Path getClasspathEntry(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        var resource = clazz.getResource(clazz.getSimpleName() + ".class");

        if (resource == null) {
            return null;
        }

        return switch (resource.getProtocol()) {
            case "jar" -> getFilePathForJarUrl(resource);
            case "file" -> getBaseDirectoryForFileUrl(resource, clazz);
            default -> null;
        };
    }

    private static Path getBaseDirectoryForFileUrl(URL resource, Class<?> clazz) {
        var pathFragment = clazz.getName()
                .replace('.', '/') + ".class";
        var path = URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8);

        var idx = path.lastIndexOf(pathFragment);

        if (idx == -1) {
            return null;
        }

        return Path.of(path.substring(0, idx));
    }

    private static Path getFilePathForJarUrl(URL url) {
        try {
            final JarURLConnection connection =
                    (JarURLConnection) url.openConnection();
            final URL resolvedUrl = connection.getJarFileURL();

            return Path.of(resolvedUrl.toURI());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Couldn't find file path for Jar URL", e);
        }
    }

}
