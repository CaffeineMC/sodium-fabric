package me.jellysquid.mods.sodium.client.gui;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jellysquid.mods.sodium.client.gui.options.TextProvider;

import java.io.*;
import java.util.function.Supplier;

public class SodiumGameOptions {
    public final QualitySettings quality = new QualitySettings();
    public final PerformanceSettings performance = new PerformanceSettings();

    public static class PerformanceSettings {
        public boolean useVAOs = true;
        public boolean useLargeBuffers = true;
        public boolean useFogChunkCulling = true;
    }

    public static class QualitySettings {
        public GraphicsQuality cloudQuality = GraphicsQuality.DEFAULT;
        public GraphicsQuality leavesQuality = GraphicsQuality.DEFAULT;
        public GraphicsQuality weatherQuality = GraphicsQuality.DEFAULT;
        public GraphicsQuality translucentBlockQuality = GraphicsQuality.DEFAULT;

        public MipmapQuality mipmapQuality = MipmapQuality.NEAREST;

        public boolean enableVignette = true;
        public boolean enableFog = true;
        public boolean enableClouds = true;
    }

    public enum DefaultGraphicsQuality implements TextProvider {
        FAST("Fast"),
        FANCY("Fancy");

        private final String name;

        DefaultGraphicsQuality(String name) {
            this.name = name;
        }

        @Override
        public String getLocalizedName() {
            return this.name;
        }
    }

    public enum GraphicsQuality implements TextProvider {
        DEFAULT("Default"),
        FAST("Fast"),
        FANCY("Fancy");

        private final String name;

        GraphicsQuality(String name) {
            this.name = name;
        }

        @Override
        public String getLocalizedName() {
            return this.name;
        }

        public boolean isFancy() {
            return this == FANCY;
        }

        public boolean isFancy(boolean def) {
            return this == DEFAULT ? def : this.isFancy();
        }
    }

    public enum MipmapQuality implements TextProvider {
        NEAREST("Nearest"),
        LINEAR("Linear"),
        BI_LINEAR("Bi-Linear"),
        TRI_LINEAR("Tri-Linear");

        private final String name;

        MipmapQuality(String name) {
            this.name = name;
        }

        @Override
        public String getLocalizedName() {
            return this.name;
        }
    }

    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();

    public static <T> T load(File file, Class<T> type, Supplier<T> defaultFactory) {
        T config;

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                config = gson.fromJson(reader, type);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse config", e);
            }
        } else {
            writeConfig(file, config = defaultFactory.get());
        }

        return config;
    }

    private static <T> void writeConfig(File file, T config) {
        File dir = file.getParentFile();

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Could not create parent directories");
            }
        } else if (!dir.isDirectory()) {
            throw new RuntimeException("The parent file is not a directory");
        }

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            throw new RuntimeException("Could not save configuration file", e);
        }
    }
}
