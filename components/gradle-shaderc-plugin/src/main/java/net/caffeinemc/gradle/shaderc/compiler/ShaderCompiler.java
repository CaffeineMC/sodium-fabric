package net.caffeinemc.gradle.shaderc.compiler;

import net.caffeinemc.gradle.shaderc.compiler.options.ShaderCompilerOptions;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;

import java.io.IOException;
import java.lang.ref.Cleaner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ShaderCompiler {
    private static final Cleaner CLEANER = Cleaner.create();

    private final long handle;

    public ShaderCompiler() {
        var handle = Shaderc.shaderc_compiler_initialize();

        if (handle == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to initialize compiler");
        }

        CLEANER.register(this, () -> Shaderc.shaderc_compiler_release(handle));

        this.handle = handle;
    }

    public byte[] compile(Path inputFile, String inputFileName, int inputFileType, ShaderCompilerOptions options) {
        String source;

        try {
            source = Files.readString(inputFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't read shader source", e);
        }

        var result = Shaderc.shaderc_compile_into_spv(this.handle, source, inputFileType, inputFileName, "main", options.handle());

        if (result == MemoryUtil.NULL) {
            throw new RuntimeException("Couldn't create compiler object");
        }

        try {
            var message = Shaderc.shaderc_result_get_error_message(result);

            if (message != null && !message.isBlank()) {
                System.err.println(message);
            }

            if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success) {
                throw new RuntimeException("Failed to compile shader");
            }

            return getResultData(result);
        } finally {
            Shaderc.shaderc_result_release(result);
        }
    }

    private static byte[] getResultData(long result) {
        var buffer = Shaderc.shaderc_result_get_bytes(result);

        if (buffer == null) {
            throw new NullPointerException("No binary object returned by the compiler");
        }

        var data = new byte[buffer.remaining()];
        buffer.get(data);

        return data;
    }
}
