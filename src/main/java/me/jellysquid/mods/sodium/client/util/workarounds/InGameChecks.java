package me.jellysquid.mods.sodium.client.util.workarounds;

import me.jellysquid.mods.sodium.client.gui.console.Console;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class InGameChecks {

    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-InGameChecks");
    private static final Path resourcePackDir = MinecraftClient.getInstance().getResourcePackDir();
    private static final List<String> vshBlacklist = new ArrayList<>(Arrays.asList(
            "rendertype_solid.vsh",
            "rendertype_cutout_mipped.vsh",
            "rendertype_cutout.vsh",
            "rendertype_translucent.vsh",
            "rendertype_tripwire.vsh"
    ));
    private static final List<String> glslBlacklist = new ArrayList<>(Arrays.asList(
            "light.glsl",
            "fog.glsl"
    ));

    /**
     * <a href="https://github.com/CaffeineMC/sodium-fabric/issues/1569">#1569</a>
     * Iterate through all active resource packs, and detect resource packs which contain files matching the blacklist.
     * An error message is shown for resource packs which replace terrain core shaders.
     * A warning is shown for resource packs which modify the default light.glsl and fog.glsl shaders.
     * Detailed information on shader files replaced or modified by resource packs is printed in the client log.
     */
    public static void checkIfCoreShaderLoaded() {
        Collection<String> activeResourcePacks = MinecraftClient.getInstance().getResourcePackManager().getEnabledNames();
        HashMap<String, MessageLevel> detectedResourcePacks = new HashMap<>();
		
        for (String s : activeResourcePacks) {
			
            if (s.startsWith("file/")) {
                String resourcePackName = s.substring(5);
				
                try (ZipFile zip = new ZipFile(Paths.get(resourcePackDir + "\\" + resourcePackName).toFile())) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();

                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String fileName = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
						
                        if (vshBlacklist.contains(fileName)) {
							
                            if (!detectedResourcePacks.containsKey(resourcePackName)) {
                                detectedResourcePacks.put(resourcePackName, MessageLevel.SEVERE);
                            } else if (detectedResourcePacks.get(resourcePackName) == MessageLevel.WARN) {
                                detectedResourcePacks.replace(resourcePackName, MessageLevel.SEVERE);
                            }
							
                            logMessageError("Resource pack '" + resourcePackName + "' replaces core shader '" + fileName + "'");
                        }
						
                        if (glslBlacklist.contains(fileName)) {
							
                            if (!detectedResourcePacks.containsKey(resourcePackName)) {
                                detectedResourcePacks.put(resourcePackName, MessageLevel.WARN);
                            }
							
                            logMessageWarn("Resource pack '" + resourcePackName + "' modifies shader '" + fileName + "'");
                        }
                    }
		
                } catch (Exception e) {
                    LOGGER.error("Could not read resource pack '" + resourcePackName + "'", e);
                }
            }
        }
		
        if (detectedResourcePacks.containsValue(MessageLevel.SEVERE)) {
            showConsoleMessage(Text.translatable("sodium.console.core_shaders_error"), MessageLevel.SEVERE);
			
            for (Map.Entry<String, MessageLevel> entry : detectedResourcePacks.entrySet()) {
				
                if (entry.getValue() == MessageLevel.SEVERE) {
                    showConsoleMessage(Text.literal(entry.getKey()), MessageLevel.SEVERE);
                }
            }
        }
		
        if (detectedResourcePacks.containsValue(MessageLevel.WARN)) {
            showConsoleMessage(Text.translatable("sodium.console.core_shaders_warn"), MessageLevel.WARN);
			
            for (Map.Entry<String, MessageLevel> entry : detectedResourcePacks.entrySet()) {
				
                if (entry.getValue() == MessageLevel.WARN) {
                    showConsoleMessage(Text.literal(entry.getKey()), MessageLevel.WARN);
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

    private static void logMessageError(String message, Object... args) {
        LOGGER.error(message, args);
    }

    private static void logMessageWarn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

}
