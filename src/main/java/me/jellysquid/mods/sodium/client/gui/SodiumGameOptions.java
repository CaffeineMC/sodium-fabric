package me.jellysquid.mods.sodium.client.gui;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.options.TextProvider;
import me.jellysquid.mods.sodium.client.render.chunk.backends.gl20.GL20ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.backends.gl30.GL30ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.backends.gl43.GL43ChunkRenderBackend;
import net.minecraft.client.options.GraphicsMode;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class SodiumGameOptions {
    public final QualitySettings quality = new QualitySettings();
    public final AdvancedSettings advanced = new AdvancedSettings();

    private Path configPath;

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

        private final Text text;
        private final SupportCheck supportedFunc;

        ChunkRendererBackendOption(String name, SupportCheck supportedFunc) {
            this.text = new LiteralText(name);
            this.supportedFunc = supportedFunc;
        }

        @Override
        public Text getText() {
            return this.text;
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
        DEFAULT("generator.default"),
        FANCY("options.clouds.fancy"),
        FAST("options.clouds.fast");

        private final Text text;

        GraphicsQuality(String name) {
            this.text = new TranslatableText(name);
        }

        @Override
        public Text getText() {
            return this.text;
        }

        public boolean isFancy(GraphicsMode graphicsMode) {
            return (this == FANCY) || (this == DEFAULT && (graphicsMode == GraphicsMode.FANCY || graphicsMode == GraphicsMode.FABULOUS));
        }
    }

    public enum LightingQuality implements TextProvider {
        HIGH("options.ao.max"),
        LOW("options.ao.min"),
        OFF("options.ao.off");

        private final Text text;

        LightingQuality(String text) {
            this.text = new TranslatableText(text);
        }

        @Override
        public Text getText() {
            return this.text;
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static SodiumGameOptions load(Path path) {
        SodiumGameOptions config;

        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                config = GSON.fromJson(reader, SodiumGameOptions.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse config", e);
            }

            config.sanitize();
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

    private void sanitize() {
        if (this.advanced.chunkRendererBackend == null) {
            this.advanced.chunkRendererBackend = ChunkRendererBackendOption.BEST;
        }
    }

    public void writeChanges() throws IOException {
        Path dir = this.configPath.getParent();

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        } else if (!Files.isDirectory(dir)) {
            throw new IOException("Not a directory: " + dir);
        }

        Files.write(this.configPath, GSON.toJson(this)
                .getBytes(StandardCharsets.UTF_8));
    }
}
