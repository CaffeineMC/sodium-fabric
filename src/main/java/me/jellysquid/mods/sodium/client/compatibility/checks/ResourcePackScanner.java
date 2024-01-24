package me.jellysquid.mods.sodium.client.compatibility.checks;

import me.jellysquid.mods.sodium.client.gui.console.Console;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ResourcePackScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-ResourcePackScanner");

    private static final Set<String> SHADER_PROGRAM_BLACKLIST = Set.of(
            "rendertype_solid.vsh",
            "rendertype_solid.fsh",
            "rendertype_solid.json",
            "rendertype_cutout_mipped.vsh",
            "rendertype_cutout_mipped.fsh",
            "rendertype_cutout_mipped.json",
            "rendertype_cutout.vsh",
            "rendertype_cutout.fsh",
            "rendertype_cutout.json",
            "rendertype_translucent.vsh",
            "rendertype_translucent.fsh",
            "rendertype_translucent.json",
            "rendertype_tripwire.vsh",
            "rendertype_tripwire.fsh",
            "rendertype_tripwire.json"
    );

    private static final Set<String> SHADER_INCLUDE_BLACKLIST = Set.of(
            "light.glsl",
            "fog.glsl"
    );

    /**
     * <a href="https://github.com/CaffeineMC/sodium-fabric/issues/1569">#1569</a>
     * Iterate through all active resource packs, and detect resource packs which contain files matching the blacklist.
     * An error message is shown for resource packs which replace terrain core shaders.
     * A warning is shown for resource packs which replace the default light.glsl and fog.glsl shaders.
     * Detailed information on shader files replaced by resource packs is printed in the client log.
     */
    public static void checkIfCoreShaderLoaded(ResourceManager manager) {
        var outputs = manager.streamResourcePacks()
                .filter(pack -> !isBuiltInResourcePack(pack))
                .collect(Collectors.toMap(ResourcePack::getName, ResourcePackScanner::scanResources));

        printToasts(outputs);
        printCompatibilityReport(outputs);
    }

    private static void printToasts(Map<String, ScanResults> scanResults) {
        var incompatibleResourcePacks = new ArrayList<String>();
        var likelyIncompatibleResourcePacks = new ArrayList<String>();

        for (var entry : scanResults.entrySet()) {
            var path = entry.getKey();
            var result = entry.getValue();

            if (!result.shaderPrograms.isEmpty()) {
                incompatibleResourcePacks.add(path);
            } else if (!result.shaderIncludes.isEmpty()) {
                likelyIncompatibleResourcePacks.add(path);
            }
        }

        boolean shown = false;

        if (!incompatibleResourcePacks.isEmpty()) {
            showConsoleMessage(Text.translatable("sodium.console.core_shaders_error"), MessageLevel.SEVERE);

            for (var pack : incompatibleResourcePacks) {
                showConsoleMessage(Text.literal(getResourcePackName(pack)), MessageLevel.SEVERE);
            }

            shown = true;
        }

        if (!likelyIncompatibleResourcePacks.isEmpty()) {
            showConsoleMessage(Text.translatable("sodium.console.core_shaders_warn"), MessageLevel.WARN);

            for (var pack : likelyIncompatibleResourcePacks) {
                showConsoleMessage(Text.literal(getResourcePackName(pack)), MessageLevel.WARN);
            }

            shown = true;
        }

        if (shown) {
            showConsoleMessage(Text.translatable("sodium.console.core_shaders_info"), MessageLevel.INFO);
        }
    }

    private static void printCompatibilityReport(Map<String, ScanResults> scanResults) {
        var builder = new StringBuilder();

        for (var entry : scanResults.entrySet()) {
            var path = entry.getKey();
            var result = entry.getValue();

            builder.append("- Resource pack: ").append(getResourcePackName(path)).append("\n");

            if (!result.shaderPrograms.isEmpty()) {
                emitProblem(builder,
                        "The resource pack replaces terrain shaders, which are not supported",
                        "https://github.com/CaffeineMC/sodium-fabric/wiki/Resource-Packs",
                        result.shaderPrograms);
            }

            if (!result.shaderIncludes.isEmpty()) {
                emitProblem(builder,
                        "The resource pack modifies shader include files, which are not fully supported",
                        "https://github.com/CaffeineMC/sodium-fabric/wiki/Resource-Packs",
                        result.shaderIncludes);
            }
        }

        if (!builder.isEmpty()) {
            LOGGER.error("The following compatibility issues were found with installed resource packs:\n{}", builder);
        }
    }

    private static void emitProblem(StringBuilder builder, String description, String url, List<String> resources) {
        builder.append("\t- Problem found: ").append("\n");
        builder.append("\t\t- Description:\n\t\t\t").append(description).append("\n");
        builder.append("\t\t- More information: ").append(url).append("\n");
        builder.append("\t\t- Files: ").append("\n");

        for (var resource : resources) {
            builder.append("\t\t\t- ").append(resource).append("\n");
        }
    }

    @NotNull
    private static ScanResults scanResources(ResourcePack pack) {
        final var ignoredShaders = determineIgnoredShaders(pack);

        if (!ignoredShaders.isEmpty()) {
            LOGGER.warn("Resource pack '{}' indicates the following shaders should be ignored: {}",
                    getResourcePackName(pack.getName()), String.join(", ", ignoredShaders));
        }

        final var unsupportedShaderPrograms = new ArrayList<String>();
        final var unsupportedShaderIncludes = new ArrayList<String>();

        pack.findResources(ResourceType.CLIENT_RESOURCES, Identifier.DEFAULT_NAMESPACE, "shaders", (identifier, supplier) -> {
            // Trim full shader file path to only contain the filename
            final var path = identifier.getPath();
            final var name = path.substring(path.lastIndexOf('/') + 1);

            // Check if the pack has already acknowledged the warnings in this file,
            // in this case we report a different info log about the situation
            if (ignoredShaders.contains(name)) {
                return;
            }

            // Check the path against known problem files
            if (SHADER_PROGRAM_BLACKLIST.contains(name)) {
                unsupportedShaderPrograms.add(path);
            } else if (SHADER_INCLUDE_BLACKLIST.contains(name)) {
                unsupportedShaderIncludes.add(path);
            }
        });

        return new ScanResults(unsupportedShaderPrograms, unsupportedShaderIncludes);
    }

    private static boolean isBuiltInResourcePack(ResourcePack pack) {
        var name = pack.getName();
        return name.equals("vanilla") || name.equals("fabric");
    }

    private static String getResourcePackName(String path) {
        // Omit 'file/' prefix for the in-game message
        return path.startsWith("file/") ? path.substring(5) : path;
    }

    /**
     * Looks at a resource pack's metadata to find a list of shaders that can be gracefully
     * ignored. This offers resource packs the ability to acknowledge they are shipping shaders
     * which will not work with Sodium, but that Sodium can ignore.
     *
     * @param resourcePack The resource pack to fetch the ignored shaders of
     * @return A list of shaders to ignore, this is the filename only without the path
     */
    private static List<String> determineIgnoredShaders(ResourcePack resourcePack) {
        var ignoredShaders = new ArrayList<String>();
        try {
            var meta = resourcePack.parseMetadata(SodiumResourcePackMetadata.SERIALIZER);
            if (meta != null) {
                ignoredShaders.addAll(meta.ignoredShaders());
            }
        } catch (IOException x) {
            LOGGER.error("Failed to load pack.mcmeta file for resource pack '{}'", resourcePack.getName());
        }
        return ignoredShaders;
    }

    private static void showConsoleMessage(MutableText message, MessageLevel messageLevel) {
        Console.instance().logMessage(messageLevel, message, 12.5);
    }

    private record ScanResults(ArrayList<String> shaderPrograms, ArrayList<String> shaderIncludes) {

    }
}
