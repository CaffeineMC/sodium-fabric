package me.jellysquid.mods.sodium.client.gui;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.options.TextProvider;
import me.jellysquid.mods.sodium.client.render.chunk.backends.gl20.GL20ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.backends.gl30.GL30ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.backends.gl43.GL43ChunkRenderBackend;
import me.jellysquid.mods.sodium.common.util.PathUtil;
import net.minecraft.client.options.GraphicsMode;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.stream.Stream;

public class SodiumGameOptions {
    private static final Logger LOGGER = SodiumClientMod.logger();

    public final QualitySettings quality = new QualitySettings();
    public final AdvancedSettings advanced = new AdvancedSettings();

    private Path path;

    public void notifyListeners() {
        SodiumClientMod.onConfigChanged(this);
    }

    public static class AdvancedSettings {
        public ChunkRendererBackendOption chunkRendererBackend = ChunkRendererBackendOption.BEST;
        public boolean animateOnlyVisibleTextures = true;
        public boolean useAdvancedEntityCulling = true;
        public boolean useParticleCulling = true;
        public boolean useFogOcclusion = true;
        public boolean useCompactVertexFormat = true;
        public boolean useChunkFaceCulling = true;
        public boolean useMemoryIntrinsics = true;
        public boolean disableDriverBlacklist = false;
    }

    public static class QualitySettings {
        public GraphicsQuality cloudQuality = GraphicsQuality.DEFAULT;
        public GraphicsQuality weatherQuality = GraphicsQuality.DEFAULT;

        public boolean enableVignette = true;
        public boolean enableClouds = true;

        public LightingQuality smoothLighting = LightingQuality.HIGH;
    }

    public enum ChunkRendererBackendOption implements TextProvider {
        GL43("Multidraw (GL 4.3)", GL43ChunkRenderBackend::isSupported),
        GL30("Oneshot (GL 3.0)", GL30ChunkRenderBackend::isSupported),
        GL20("Oneshot (GL 2.0)", GL20ChunkRenderBackend::isSupported);

        public static final ChunkRendererBackendOption BEST = pickBestBackend();

        private final String name;
        private final SupportCheck supportedFunc;

        ChunkRendererBackendOption(String name, SupportCheck supportedFunc) {
            this.name = name;
            this.supportedFunc = supportedFunc;
        }

        @Override
        public String getLocalizedName() {
            return this.name;
        }

        public boolean isSupported(boolean disableBlacklist) {
            return this.supportedFunc.isSupported(disableBlacklist);
        }

        public static ChunkRendererBackendOption[] getAvailableOptions(boolean disableBlacklist) {
            return streamAvailableOptions(disableBlacklist)
                    .toArray(ChunkRendererBackendOption[]::new);
        }

        public static Stream<ChunkRendererBackendOption> streamAvailableOptions(boolean disableBlacklist) {
            return Arrays.stream(ChunkRendererBackendOption.values())
                    .filter((o) -> o.isSupported(disableBlacklist));
        }

        private static ChunkRendererBackendOption pickBestBackend() {
            return streamAvailableOptions(false)
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
        }

        private interface SupportCheck {
            boolean isSupported(boolean disableBlacklist);
        }
    }

    public enum GraphicsQuality implements TextProvider {
        DEFAULT("Default"),
        FANCY("Fancy"),
        FAST("Fast");

        private final String name;

        GraphicsQuality(String name) {
            this.name = name;
        }

        @Override
        public String getLocalizedName() {
            return this.name;
        }

        public boolean isFancy(GraphicsMode graphicsMode) {
            return (this == FANCY) || (this == DEFAULT && (graphicsMode == GraphicsMode.FANCY || graphicsMode == GraphicsMode.FABULOUS));
        }
    }

    public enum LightingQuality implements TextProvider {
        HIGH("High"),
        LOW("Low"),
        OFF("Off");

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

    public static SodiumGameOptions load(Path path) {
        SodiumGameOptions config;

        if (Files.exists(path)) {
            try (InputStream is = Files.newInputStream(path);
                 InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader reader = new BufferedReader(isr)) {
                config = gson.fromJson(reader, SodiumGameOptions.class);
            } catch (Exception e) {
                LOGGER.error("Failed to parse options file! Loading default values", e);
                config = new SodiumGameOptions();
                config.path = path;

                boolean backupSuccess = true;
                Path backupPath = PathUtil.resolveTimestampedSibling(path, "BACKUP");
                LOGGER.info("Backing up config to \"{}\"...", backupPath.toString());
                try {
                    Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException be) {
                    LOGGER.error("Failed to back up config!", be);
                    backupSuccess = false;
                }

                // write default values to file
                config.writeChanges();

                String eMsg = "Failed to parse options file! It has been replaced with the default values";
                if (backupSuccess)
                    eMsg += ", with the original backed up at \"" + backupPath + "\"";
                throw new RuntimeException(eMsg, e);
            }

            config.path = path;
            config.sanitize();
        } else {
            LOGGER.info("Could not find options file, loading default values");
            config = new SodiumGameOptions();
            config.path = path;
            config.writeChanges();
        }

        return config;
    }

    private void sanitize() {
        if (this.advanced.chunkRendererBackend == null) {
            this.advanced.chunkRendererBackend = ChunkRendererBackendOption.BEST;
        }
    }

    public void writeChanges() {
        Path dir = this.path.getParent();

        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                LOGGER.error("Could not create parent directories for \"" + dir + "\"!", e);
                return;
            }
        } else if (!Files.isDirectory(dir)) {
            LOGGER.error("Parent directory \"{}\" is, in fact, not a directory!", dir);
            return;
        }

        Path tempPath = PathUtil.resolveTimestampedSibling(this.path, "TEMP");
        try (OutputStream os = Files.newOutputStream(tempPath);
             OutputStreamWriter osw = new OutputStreamWriter(os);
             BufferedWriter writer = new BufferedWriter(osw)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            LOGGER.error("Could not save config file to \"" + tempPath + "\"!", e);
            return;
        }

        try {
            Files.move(tempPath, this.path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to move config file \"" + tempPath + "\" into place at \"" + this.path + "!", e);
        }
    }
}
