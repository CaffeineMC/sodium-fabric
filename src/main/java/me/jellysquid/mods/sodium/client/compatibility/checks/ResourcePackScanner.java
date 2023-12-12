package me.jellysquid.mods.sodium.client.compatibility.checks;

import me.jellysquid.mods.sodium.client.gui.console.Console;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourcePackScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-InGameChecks");
    private static final List<String> VSH_FSH_BLACKLIST = Arrays.asList(
            "rendertype_solid.vsh", "rendertype_solid.fsh",
            "rendertype_cutout_mipped.vsh", "rendertype_cutout_mipped.fsh",
            "rendertype_cutout.vsh", "rendertype_cutout.fsh",
            "rendertype_translucent.vsh", "rendertype_translucent.fsh",
            "rendertype_tripwire.vsh", "rendertype_tripwire.fsh"
    );
    private static final List<String> GLSL_BLACKLIST = Arrays.asList(
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
        HashMap<String, MessageLevel> detectedResourcePacks = new HashMap<>();
        var customResourcePacks = manager.streamResourcePacks();

        customResourcePacks.forEach(resourcePack -> {
            // Omit 'vanilla' and 'fabric' resource packs
            if (!resourcePack.getName().equals("vanilla") && !resourcePack.getName().equals("fabric")) {
                var resourcePackName = resourcePack.getName();

                // Offer resource packs the ability to acknowledge certain shaders it knows can cause issues
                // but has manually checked for issues, we read this from an optional part of the pack.mcmeta file
                var acknowledgedFiles = new ArrayList<String>();
                try {
                    var meta = resourcePack.parseMetadata(SafeShadersMetadata.SERIALIZER);
                    if (meta != null) {
                        acknowledgedFiles.addAll(meta.names());
                    }
                } catch (IOException x) {
                    LOGGER.error("Failed to load pack.mcmeta file for resource pack '" + resourcePackName + "'", x);
                }

                resourcePack.findResources(ResourceType.CLIENT_RESOURCES, Identifier.DEFAULT_NAMESPACE, "shaders", (path, ignored) -> {
                    // Trim full shader file path to only contain the filename
                    var shaderName = path.getPath().substring(path.getPath().lastIndexOf('/') + 1);

                    // Check if the pack has already acknowledged the warnings in this file,
                    // in this case we report a different info log about the situation
                    if (acknowledgedFiles.contains(shaderName)) {
                        if (VSH_FSH_BLACKLIST.contains(shaderName)) {
                            LOGGER.info("Resource pack '" + resourcePackName + "' replaces core shader '" + shaderName
                                    + "' but indicates it should not cause issues. Please notify the pack author first" +
                                    " if you experience any issues.");
                        }

                        if (GLSL_BLACKLIST.contains(shaderName)) {
                            LOGGER.info("Resource pack '" + resourcePackName + "' replaces shader '" + shaderName
                                    + "' but indicates it should not cause issues. Please notify the pack author first" +
                                    " if you experience any issues.");
                        }
                        return;
                    }

                    if (VSH_FSH_BLACKLIST.contains(shaderName)) {

                        if (!detectedResourcePacks.containsKey(resourcePackName)) {
                            detectedResourcePacks.put(resourcePackName, MessageLevel.SEVERE);
                        } else if (detectedResourcePacks.get(resourcePackName) == MessageLevel.WARN) {
                            detectedResourcePacks.replace(resourcePackName, MessageLevel.SEVERE);
                        }

                        LOGGER.error("Resource pack '" + resourcePackName + "' replaces core shader '" + shaderName + "'");
                    }

                    if (GLSL_BLACKLIST.contains(shaderName)) {

                        if (!detectedResourcePacks.containsKey(resourcePackName)) {
                            detectedResourcePacks.put(resourcePackName, MessageLevel.WARN);
                        }

                        LOGGER.warn("Resource pack '" + resourcePackName + "' replaces shader '" + shaderName + "'");

                    }
                });
            }
        });

        if (detectedResourcePacks.containsValue(MessageLevel.SEVERE)) {
            showConsoleMessage(Text.translatable("sodium.console.core_shaders_error"), MessageLevel.SEVERE);

            for (Map.Entry<String, MessageLevel> entry : detectedResourcePacks.entrySet()) {

                if (entry.getValue() == MessageLevel.SEVERE) {
                    // Omit 'file/' prefix for the in-game message
                    var message = entry.getKey().startsWith("file/") ? entry.getKey().substring(5) : entry.getKey();
                    showConsoleMessage(Text.literal(message), MessageLevel.SEVERE);
                }
            }
        }

        if (detectedResourcePacks.containsValue(MessageLevel.WARN)) {
            showConsoleMessage(Text.translatable("sodium.console.core_shaders_warn"), MessageLevel.WARN);

            for (Map.Entry<String, MessageLevel> entry : detectedResourcePacks.entrySet()) {

                if (entry.getValue() == MessageLevel.WARN) {
                    // Omit 'file/' prefix for the in-game message
                    var message = entry.getKey().startsWith("file/") ? entry.getKey().substring(5) : entry.getKey();
                    showConsoleMessage(Text.literal(message), MessageLevel.WARN);
                }
            }
        }

        if (!detectedResourcePacks.isEmpty()) {
            showConsoleMessage(Text.translatable("sodium.console.core_shaders_info"), MessageLevel.INFO);
        }
    }

    private static void showConsoleMessage(MutableText message, MessageLevel messageLevel) {
        Console.instance().logMessage(messageLevel, message, 20.0);
    }

}
