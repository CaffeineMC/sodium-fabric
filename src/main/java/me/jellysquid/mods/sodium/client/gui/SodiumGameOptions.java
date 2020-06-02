package me.jellysquid.mods.sodium.client.gui;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jellysquid.mods.sodium.client.gui.options.TextProvider;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;

public class SodiumGameOptions {
    public final QualitySettings quality = new QualitySettings();
    public final PerformanceSettings performance = new PerformanceSettings();

    private File file;

    public static class PerformanceSettings {
        public boolean useVertexArrays = true;
        public boolean useLargeBuffers = false;
        public boolean animateOnlyVisibleTextures = true;
        public boolean useAdvancedEntityCulling = true;
        public boolean useImmutableStorage = true;
        public boolean useParticleCulling = true;
        public boolean useFogOcclusion = true;
    }

    public static class QualitySettings {
        public GraphicsQuality cloudQuality = GraphicsQuality.DEFAULT;
        public GraphicsQuality weatherQuality = GraphicsQuality.DEFAULT;

        public MipmapQuality mipmapQuality = MipmapQuality.NEAREST;

        public boolean enableVignette = true;
        public boolean enableFog = true;
        public boolean enableClouds = true;

        public LightingQuality smoothLighting = LightingQuality.HIGH;
        public int biomeBlendDistance = 3;
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

    public enum LightingQuality implements TextProvider {
        OFF("Off"),
        LOW("Low"),
        HIGH("High");

        private final String name;

        LightingQuality(String name) {
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
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static SodiumGameOptions load(File file) {
        SodiumGameOptions config;

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                config = gson.fromJson(reader, SodiumGameOptions.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse config", e);
            }
        } else {
            config = new SodiumGameOptions();
        }

        config.file = file;
        config.writeChanges();

        return config;
    }

    public void writeChanges() {
        File dir = this.file.getParentFile();

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Could not create parent directories");
            }
        } else if (!dir.isDirectory()) {
            throw new RuntimeException("The parent file is not a directory");
        }

        try (FileWriter writer = new FileWriter(this.file)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            throw new RuntimeException("Could not save configuration file", e);
        }
    }
}
