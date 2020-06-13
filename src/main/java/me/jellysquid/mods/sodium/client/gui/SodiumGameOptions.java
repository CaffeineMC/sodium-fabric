package me.jellysquid.mods.sodium.client.gui;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jellysquid.mods.sodium.client.gui.options.TextProvider;
import me.jellysquid.mods.sodium.client.render.chunk.backends.gl20.GL20ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.backends.gl30.GL30ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.backends.gl46.GL46ChunkRenderBackend;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

public class SodiumGameOptions {
    public final QualitySettings quality = new QualitySettings();
    public final PerformanceSettings performance = new PerformanceSettings();

    private File file;

    public static class PerformanceSettings {
        public ChunkRendererBackendOption chunkRendererBackend = ChunkRendererBackendOption.DEFAULT;
        public boolean animateOnlyVisibleTextures = true;
        public boolean useAdvancedEntityCulling = true;
        public boolean useParticleCulling = true;
        public boolean useFogOcclusion = true;
        public boolean useCompactVertexFormat = true;
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

    public enum ChunkRendererBackendOption implements TextProvider {
        GL46("OpenGL 4.6", GL46ChunkRenderBackend::isSupported),
        GL30("OpenGL 3.0", GL30ChunkRenderBackend::isSupported),
        GL20("OpenGL 2.0", GL20ChunkRenderBackend::isSupported);

        public static final ChunkRendererBackendOption DEFAULT = pickBestBackend();

        private final String name;
        private final BooleanSupplier supportedFunc;

        ChunkRendererBackendOption(String name, BooleanSupplier supportedFunc) {
            this.name = name;
            this.supportedFunc = supportedFunc;
        }

        @Override
        public String getLocalizedName() {
            return this.name;
        }

        public boolean isSupported() {
            return this.supportedFunc.getAsBoolean();
        }

        public static ChunkRendererBackendOption[] getAvailableOptions() {
            return streamAvailableOptions()
                    .toArray(ChunkRendererBackendOption[]::new);
        }

        public static Stream<ChunkRendererBackendOption> streamAvailableOptions() {
            return Arrays.stream(ChunkRendererBackendOption.values())
                    .filter(ChunkRendererBackendOption::isSupported);
        }

        private static ChunkRendererBackendOption pickBestBackend() {
            return streamAvailableOptions()
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
        }
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

            config.sanitize();
        } else {
            config = new SodiumGameOptions();
        }

        config.file = file;
        config.writeChanges();

        return config;
    }

    private void sanitize() {
        if (this.performance.chunkRendererBackend == null) {
            this.performance.chunkRendererBackend = ChunkRendererBackendOption.DEFAULT;
        }
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
