package net.caffeinemc.gradle.shaderc;

import net.caffeinemc.gradle.shaderc.compiler.IncludeResolver;
import net.caffeinemc.gradle.shaderc.compiler.ShaderCompiler;
import net.caffeinemc.gradle.shaderc.compiler.options.CompilerIdentifier;
import net.caffeinemc.gradle.shaderc.compiler.options.ShaderCompilerOptions;
import net.caffeinemc.gradle.shaderc.compiler.options.ShaderOptimizationLevel;
import net.caffeinemc.gradle.shaderc.compiler.options.ShaderProfile;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.lwjgl.util.shaderc.Shaderc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class CompileTask extends DefaultTask {
    private static final Map<String, Integer> SHADER_TYPES = Map.of(
            "vert", Shaderc.shaderc_glsl_vertex_shader,
            "tesc", Shaderc.shaderc_glsl_tess_control_shader,
            "tese", Shaderc.shaderc_glsl_tess_evaluation_shader,
            "geom", Shaderc.shaderc_glsl_geometry_shader,
            "frag", Shaderc.shaderc_glsl_fragment_shader,
            "comp", Shaderc.shaderc_glsl_compute_shader);

    @InputDirectory
    public abstract DirectoryProperty getInputFiles();

    @InputDirectory
    public abstract DirectoryProperty getIncludeFiles();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @TaskAction
    public void run() {
        var includeSourceSet = this.getIncludeFiles().get().getAsFile().toPath();
        var inputSourceSet = this.getInputFiles().get().getAsFile().toPath();
        var outputDirectory = this.getOutputDirectory().get().getAsFile().toPath();

        var resolver = new IncludeResolver(List.of(includeSourceSet));

        var options = new ShaderCompilerOptions(resolver);
        options.setVersion(new CompilerIdentifier(ShaderProfile.Core, 460));
        options.setOptimizationLevel(ShaderOptimizationLevel.Performance);
        options.enablePedantry();

        Stream<Path> inputFileStream;

        try {
            inputFileStream = Files.walk(inputSourceSet);
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk input directory", e);
        }

        inputFileStream
                .filter(inputFile -> !inputFile.startsWith(includeSourceSet))
                .map(inputFile -> {
                    var inputFileExtension = FilenameUtils.getExtension(inputFile.toString());
                    var inputFileType = SHADER_TYPES.get(inputFileExtension);

                    if (inputFileType == null) {
                        return null;
                    }

                    var inputFileRelativePath = inputSourceSet.relativize(inputFile);
                    var outputFile = outputDirectory.resolve(inputFileRelativePath + ".spv");

                    return new CompileJob(inputFile, outputFile, inputFileRelativePath.toString(), inputFileType);
                })
                .filter(Objects::nonNull)
                .parallel()
                .forEach((job) -> {
                    var data = ShaderPlugin.COMPILER.compile(job.sourceFile, job.name, job.shaderType, options);

                    try {
                        var directory = job.destinationFile.getParent();

                        if (directory != null) {
                            Files.createDirectories(directory);
                        }

                        Files.write(job.destinationFile, data);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to write output file: " + job.destinationFile, e);
                    }
                });
    }

    private record CompileJob(Path sourceFile, Path destinationFile, String name, int shaderType) {

    }
}
