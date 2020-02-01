package me.jellysquid.mods.sodium.common.config;

import me.jellysquid.mods.sodium.common.config.annotations.Category;
import me.jellysquid.mods.sodium.common.config.annotations.Option;
import me.jellysquid.mods.sodium.common.config.parser.ConfigParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Documentation of these options: https://github.com/jellysquid3/Sodium/wiki/Configuration-File
 */
@SuppressWarnings("CanBeFinal")
public class SodiumConfig {
    @Category("general")
    public static class GeneralConfig {
        @Option("use_fast_thread_assertions")
        public boolean useFastThreadAssertions = true;

        @Option("use_matrix_pooling")
        public boolean useMatrixPooling = true;
    }

    @Category("chunk")
    public static class ChunkConfig {
        @Option("use_fast_ao")
        public boolean useFastAo = true;

        @Option("use_fast_block_occlusion_cache")
        public boolean useFastBlockOcclusionCache = true;
    }

    @Category("pipeline")
    public static class PipelineConfig {
        @Option("avoid_enum_cloning")
        public boolean avoidEnumCloning = true;

        @Option("use_fast_vertex_consumer")
        public boolean useFastVertexConsumer = true;
    }

    public final GeneralConfig general = new GeneralConfig();
    public final ChunkConfig chunk = new ChunkConfig();
    public final PipelineConfig pipeline = new PipelineConfig();

    /**
     * Loads the configuration file from the specified location. If it does not exist, a new configuration file will be
     * created. The file on disk will then be updated to include any new options.
     */
    public static SodiumConfig load(File file) {
        if (!file.exists()) {
            writeDefaultConfig(file);

            return new SodiumConfig();
        }

        try {
            return ConfigParser.deserialize(SodiumConfig.class, file);
        } catch (ConfigParser.ParseException e) {
            throw new RuntimeException("Could not parse config", e);
        }
    }

    private static void writeDefaultConfig(File file) {
        File dir = file.getParentFile();

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Could not create parent directories");
            }
        } else if (!dir.isDirectory()) {
            throw new RuntimeException("The parent file is not a directory");
        }

        try (Writer writer = new FileWriter(file)) {
            writer.write("# This is the configuration file for Sodium.\n");
            writer.write("#\n");
            writer.write("# You can find information on editing this file and all the available options here:\n");
            writer.write("# https://github.com/jellysquid3/Sodium/wiki/Configuration-File\n");
            writer.write("#\n");
            writer.write("# By default, this file will be empty except for this notice.\n");
        } catch (IOException e) {
            throw new RuntimeException("Could not write default config", e);
        }
    }
}
