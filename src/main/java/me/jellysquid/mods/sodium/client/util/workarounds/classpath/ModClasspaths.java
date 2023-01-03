package me.jellysquid.mods.sodium.client.util.workarounds.classpath;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.ModOrigin;

import java.nio.file.Path;
import java.util.List;

public class ModClasspaths {
    public static List<Path> getClasspathEntriesForMod(Class<?> clazz, String modId) {
        if (shouldUseModContainerApi()) {
            return queryModContainerForFilePaths(modId);
        }

        return List.of(LibraryClasspaths.getClasspathEntry(clazz));
    }

    /**
     * This only works on Quilt 0.18 and later, since Quilt 0.17 did not implement the required functionality.
     * This should work on every version of Fabric since the API was introduced.
     */
    private static List<Path> queryModContainerForFilePaths(String modId) {
        ModContainer mod = FabricLoader.getInstance()
                .getModContainer(modId)
                .orElseThrow(() -> new NullPointerException("Could not find container for mod '%s'".formatted(modId)));

        var origin = mod.getOrigin();

        if (origin.getKind() != ModOrigin.Kind.PATH) {
            throw new RuntimeException("Mod '%s' comes from unexpected origin kind: %s".formatted(modId, origin.getKind()));
        }

        var paths = origin.getPaths();

        if (paths.isEmpty()) {
            throw new RuntimeException("Couldn't find any paths providing mod '%s'".formatted(modId));
        }

        return paths;
    }

    private static boolean shouldUseModContainerApi() {
        return FabricLoader.getInstance()
                .getModContainer("quilt_loader")
                .map(container -> {
                    var version = container.getMetadata()
                            .getVersion();

                    try {
                        if (version.compareTo(Version.parse("0.18.0")) < 0) {
                            return false;
                        }
                    } catch (VersionParsingException ignored) { }

                    return true;
                })
                .orElse(true);
    }
}
