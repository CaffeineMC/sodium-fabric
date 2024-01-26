package me.jellysquid.mods.sodium.client.gui;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import me.jellysquid.mods.sodium.client.gui.options.TextProvider;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.text.Text;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SodiumGameOptions {
    private static final String DEFAULT_FILE_NAME = "sodium-options.json";

    public final QualitySettings quality = new QualitySettings();
    public final AdvancedSettings advanced = new AdvancedSettings();
    public final PerformanceSettings performance = new PerformanceSettings();
    public final NotificationSettings notifications = new NotificationSettings();

    private boolean readOnly;

    private Path configPath;

    public static SodiumGameOptions defaults() {
        var options = new SodiumGameOptions();
        options.configPath = getConfigPath(DEFAULT_FILE_NAME);

        return options;
    }

    public static class PerformanceSettings {
        public int chunkBuilderThreads = 0;
        @SerializedName("always_defer_chunk_updates_v2") // this will reset the option in older configs
        public boolean alwaysDeferChunkUpdates = true;

        public boolean animateOnlyVisibleTextures = true;
        public boolean useEntityCulling = true;
        public boolean useFogOcclusion = true;
        public boolean useBlockFaceCulling = true;
        public boolean useNoErrorGLContext = true;

        public SortBehavior sortBehavior = SortBehavior.DYNAMIC_DEFER_NEARBY_ONE_FRAME;
    }

    public enum SortBehavior implements TextProvider {
        OFF("options.off", "OFF", SortMode.NONE),
        STATIC("sodium.options.sort_behavior.reduced", "S", SortMode.STATIC),
        DYNAMIC_DEFER_ALWAYS("sodium.options.defer_sorting.df", "DF", PriorityMode.NONE, DeferMode.ALWAYS),
        DYNAMIC_DEFER_NEARBY_ONE_FRAME("sodium.options.defer_sorting.n1", "N1", PriorityMode.NEARBY, DeferMode.ONE_FRAME),
        DYNAMIC_DEFER_NEARBY_ZERO_FRAMES("sodium.options.defer_sorting.n0", "N0", PriorityMode.NEARBY, DeferMode.ZERO_FRAMES),
        DYNAMIC_DEFER_ALL_ONE_FRAME("sodium.options.defer_sorting.a1", "A1", PriorityMode.ALL, DeferMode.ONE_FRAME),
        DYNAMIC_DEFER_ALL_ZERO_FRAMES("sodium.options.defer_sorting.a0", "A0", PriorityMode.ALL, DeferMode.ZERO_FRAMES);

        private final Text name;
        private final String shortName;
        private final SortMode sortMode;
        private final PriorityMode priorityMode;
        private final DeferMode deferMode;

        SortBehavior(String name, String shortName, SortMode sortMode, PriorityMode priorityMode, DeferMode deferMode) {
            this.name = Text.translatable(name);
            this.shortName = shortName;
            this.sortMode = sortMode;
            this.priorityMode = priorityMode;
            this.deferMode = deferMode;
        }

        SortBehavior(String name, String shortName, SortMode sortMode) {
            this(name, shortName, sortMode, null, null);
        }

        SortBehavior(String name, String shortName, PriorityMode priorityMode, DeferMode deferMode) {
            this(name, shortName, SortMode.DYNAMIC, priorityMode, deferMode);
        }

        @Override
        public Text getLocalizedName() {
            return this.name;
        }

        public String getShortName() {
            return this.shortName;
        }

        public SortMode getSortMode() {
            return this.sortMode;
        }

        public PriorityMode getPriorityMode() {
            return this.priorityMode;
        }

        public DeferMode getDeferMode() {
            return this.deferMode;
        }

        public static enum SortMode {
            NONE, STATIC, DYNAMIC
        }

        public static enum PriorityMode {
            NONE, NEARBY, ALL
        }

        public static enum DeferMode {
            ALWAYS, ONE_FRAME, ZERO_FRAMES
        }
    }

    public static class AdvancedSettings {
        public boolean enableMemoryTracing = false;
        public boolean useAdvancedStagingBuffers = true;

        public int cpuRenderAheadLimit = 3;
    }

    public static class QualitySettings {
        public GraphicsQuality weatherQuality = GraphicsQuality.DEFAULT;
        public GraphicsQuality leavesQuality = GraphicsQuality.DEFAULT;

        public boolean enableVignette = true;
    }

    public static class NotificationSettings {
        public boolean hideDonationButton = false;
    }

    public enum GraphicsQuality implements TextProvider {
        DEFAULT("options.gamma.default"),
        FANCY("options.clouds.fancy"),
        FAST("options.clouds.fast");

        private final Text name;

        GraphicsQuality(String name) {
            this.name = Text.translatable(name);
        }

        @Override
        public Text getLocalizedName() {
            return this.name;
        }

        public boolean isFancy(GraphicsMode graphicsMode) {
            return (this == FANCY) || (this == DEFAULT && (graphicsMode == GraphicsMode.FANCY || graphicsMode == GraphicsMode.FABULOUS));
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static SodiumGameOptions load() {
        return load(DEFAULT_FILE_NAME);
    }

    public static SodiumGameOptions load(String name) {
        Path path = getConfigPath(name);
        SodiumGameOptions config;

        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                config = GSON.fromJson(reader, SodiumGameOptions.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse config", e);
            }
        } else {
            config = new SodiumGameOptions();
        }

        config.configPath = path;

        try {
            config.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't update config file", e);
        }

        return config;
    }

    private static Path getConfigPath(String name) {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve(name);
    }

    public void writeChanges() throws IOException {
        if (this.isReadOnly()) {
            throw new IllegalStateException("Config file is read-only");
        }

        Path dir = this.configPath.getParent();

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        } else if (!Files.isDirectory(dir)) {
            throw new IOException("Not a directory: " + dir);
        }

        // Use a temporary location next to the config's final destination
        Path tempPath = this.configPath.resolveSibling(this.configPath.getFileName() + ".tmp");

        // Write the file to our temporary location
        Files.writeString(tempPath, GSON.toJson(this));

        // Atomically replace the old config file (if it exists) with the temporary file
        Files.move(tempPath, this.configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public void setReadOnly() {
        this.readOnly = true;
    }

    public String getFileName() {
        return this.configPath.getFileName().toString();
    }
}
