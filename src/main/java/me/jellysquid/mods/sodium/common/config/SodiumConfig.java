package me.jellysquid.mods.sodium.common.config;

import me.jellysquid.mods.sodium.common.util.PathUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Documentation of these options: https://github.com/jellysquid3/sodium-fabric/wiki/Configuration-File
 */
@SuppressWarnings("CanBeFinal")
public class SodiumConfig {
    private static final Logger LOGGER = LogManager.getLogger("SodiumConfig");

    private static final String JSON_KEY_SODIUM_OPTIONS = "sodium:options";

    private final Map<String, Option> options = new HashMap<>();

    private SodiumConfig() {
        // Defines the default rules which can be configured by the user or other mods.
        // You must manually add a rule for any new mixins not covered by an existing package rule.
        this.addMixinRule("core", true); // TODO: Don't actually allow the user to disable this

        this.addMixinRule("features.block", true);
        this.addMixinRule("features.buffer_builder", true);
        this.addMixinRule("features.buffer_builder.fast_advance", true);
        this.addMixinRule("features.buffer_builder.fast_sort", true);
        this.addMixinRule("features.buffer_builder.intrinsics", true);
        this.addMixinRule("features.chunk_rendering", true);
        this.addMixinRule("features.debug", true);
        this.addMixinRule("features.entity", true);
        this.addMixinRule("features.entity.fast_render", true);
        this.addMixinRule("features.entity.smooth_lighting", true);
        this.addMixinRule("features.gui", true);
        this.addMixinRule("features.gui.fast_loading_screen", true);
        this.addMixinRule("features.gui.font", true);
        this.addMixinRule("features.item", true);
        this.addMixinRule("features.matrix_stack", true);
        this.addMixinRule("features.model", true);
        this.addMixinRule("features.options", true);
        this.addMixinRule("features.particle", true);
        this.addMixinRule("features.particle.cull", true);
        this.addMixinRule("features.particle.fast_render", true);
        this.addMixinRule("features.render_layer", true);
        this.addMixinRule("features.texture_tracking", true);
        this.addMixinRule("features.world_ticking", true);
    }

    /**
     * Defines a Mixin rule which can be configured by users and other mods.
     * @throws IllegalStateException If a rule with that name already exists
     * @param mixin The name of the mixin package which will be controlled by this rule
     * @param enabled True if the rule will be enabled by default, otherwise false
     */
    private void addMixinRule(String mixin, boolean enabled) {
        String name = getMixinRuleName(mixin);

        if (this.options.putIfAbsent(name, new Option(name, enabled, false)) != null) {
            throw new IllegalStateException("Mixin rule already defined: " + mixin);
        }
    }

    private void readProperties(Properties props) {
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();

            Option option = this.options.get(key);

            if (option == null) {
                LOGGER.warn("No configuration key exists with name '{}', ignoring", key);
                continue;
            }

            boolean enabled;

            if (value.equalsIgnoreCase("true")) {
                enabled = true;
            } else if (value.equalsIgnoreCase("false")) {
                enabled = false;
            } else {
                LOGGER.warn("Invalid value '{}' encountered for configuration key '{}', ignoring", value, key);
                continue;
            }

            option.setEnabled(enabled, true);
        }
    }

    private void applyModOverrides() {
        for (ModContainer container : FabricLoader.getInstance().getAllMods()) {
            ModMetadata meta = container.getMetadata();

            if (meta.containsCustomValue(JSON_KEY_SODIUM_OPTIONS)) {
                CustomValue overrides = meta.getCustomValue(JSON_KEY_SODIUM_OPTIONS);

                if (overrides.getType() != CustomValue.CvType.OBJECT) {
                    LOGGER.warn("Mod '{}' contains invalid Sodium option overrides, ignoring", meta.getId());
                    continue;
                }

                for (Map.Entry<String, CustomValue> entry : overrides.getAsObject()) {
                    this.applyModOverride(meta, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private void applyModOverride(ModMetadata meta, String name, CustomValue value) {
        Option option = this.options.get(name);

        if (option == null) {
            LOGGER.warn("Mod '{}' attempted to override option '{}', which doesn't exist, ignoring", meta.getId(), name);
            return;
        }

        if (value.getType() != CustomValue.CvType.BOOLEAN) {
            LOGGER.warn("Mod '{}' attempted to override option '{}' with an invalid value, ignoring", meta.getId(), name);
            return;
        }

        boolean enabled = value.getAsBoolean();

        // disabling the option takes precedence over enabling
        if (!enabled && option.isEnabled()) {
            option.clearModsDefiningValue();
        }

        if (!enabled || option.isEnabled() || option.getDefiningMods().isEmpty()) {
            option.addModOverride(enabled, meta.getId());
        }
    }

    /**
     * Returns the effective option for the specified class name. This traverses the package path of the given mixin
     * and checks each root for configuration rules. If a configuration rule disables a package, all mixins located in
     * that package and its children will be disabled. The effective option is that of the highest-priority rule, either
     * a enable rule at the end of the chain or a disable rule at the earliest point in the chain.
     *
     * @return Null if no options matched the given mixin name, otherwise the effective option for this Mixin
     */
    public Option getEffectiveOptionForMixin(String mixinClassName) {
        int lastSplit = 0;
        int nextSplit;

        Option rule = null;

        while ((nextSplit = mixinClassName.indexOf('.', lastSplit)) != -1) {
            String key = getMixinRuleName(mixinClassName.substring(0, nextSplit));

            Option candidate = this.options.get(key);

            if (candidate != null) {
                rule = candidate;

                if (!rule.isEnabled()) {
                    return rule;
                }
            }

            lastSplit = nextSplit + 1;
        }

        return rule;
    }

    /**
     * Loads the configuration file from the specified location. If it does not exist, a new configuration file will be
     * created. The file on disk will then be updated to include any new options.
     */
    public static SodiumConfig load(Path path) {
        if (!Files.exists(path)) {
            LOGGER.info("Could not find configuration file, loading default values");

            try {
                writeDefaultConfig(path);
            } catch (IOException e) {
                LOGGER.warn("Could not write default configuration file to \"" + path + "\"!", e);
            }

            return new SodiumConfig();
        }

        Properties props = new Properties();

        try (InputStream fin = Files.newInputStream(path)){
            props.load(fin);
        } catch (IOException e) {
            LOGGER.error("Could not load config file from \"" + path + "\"! Loading default values", e);
            Path backupPath = PathUtil.resolveTimestampedSibling(path, "BACKUP");
            LOGGER.info("Backing up config to \"{}\"...", backupPath.toString());
            try {
                Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException be) {
                LOGGER.error("Failed to back up config!", be);
            }
        }

        SodiumConfig config = new SodiumConfig();
        config.readProperties(props);
        config.applyModOverrides();

        return config;
    }

    private static void writeDefaultConfig(Path path) throws IOException {
        Path dir = path.getParent();

        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new IOException("Could not create parent directories for \"" + dir + "\"!", e);
            }
        } else if (!Files.isDirectory(dir)) {
            throw new IOException("Parent directory \"" + dir + "\" is, in fact, not a directory!");
        }

        try (OutputStream os = Files.newOutputStream(path);
             OutputStreamWriter osw = new OutputStreamWriter(os);
             BufferedWriter writer = new BufferedWriter(osw)) {
            writer.write("# This is the configuration file for Sodium.\n");
            writer.write("#\n");
            writer.write("# You can find information on editing this file and all the available options here:\n");
            writer.write("# https://github.com/jellysquid3/sodium-fabric/wiki/Configuration-File\n");
            writer.write("#\n");
            writer.write("# By default, this file will be empty except for this notice.\n");
        }
    }

    private static String getMixinRuleName(String name) {
        return "mixin." + name;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public int getOptionOverrideCount() {
        return (int) this.options.values()
                .stream()
                .filter(Option::isOverridden)
                .count();
    }
}
