package net.caffeinemc.sodium.render.shader;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.gl.GLImportProcessor;
import net.minecraft.util.Identifier;

public class ShaderParser {

    public static String parseVanillaShader(ShaderLoader<Identifier> loader, Identifier name) {
        String src = loader.getShaderSource(name);

        List<String> lines = parseVanillaShader(loader, name,src);

        return String.join("\n", lines);
    }

    public static String parseVanillaShader(ShaderLoader<Identifier> loader, Identifier name, ShaderConstants constants) {
        String src = loader.getShaderSource(name);

        List<String> lines = parseVanillaShader(loader, name,src);
        lines.addAll(1, constants.getDefineStrings());

        return String.join("\n", lines);
    }

    public static List<String> parseVanillaShader(ShaderLoader<Identifier> loader, Identifier name, String src) {
        // redirect imports to our shader loader. requires some identifier trickery to get the right path for inlined imports.
        GLImportProcessor mojImportProcessor = new GLImportProcessor() {
            private final Set<Identifier> visitedImports = new ObjectOpenHashSet<>();

            @Override
            public String loadImport(boolean inline, String importName) {
                Identifier importIdentifier;
                if (inline) {
                    String shaderPath = name.getPath();
                    String shaderDir = shaderPath.substring(0, shaderPath.lastIndexOf('/') + 1);
                    importIdentifier = new Identifier(name.getNamespace(), shaderDir + importName);
                } else {
                    importIdentifier = new Identifier(Identifier.DEFAULT_NAMESPACE, "include/" + importName);
                }
                if (!this.visitedImports.add(importIdentifier)) {
                    return null;
                }

                return loader.getShaderSource(importIdentifier);
            }
        };

        return mojImportProcessor.readSource(src);
    }

    public static <T> String parseSodiumShader(ShaderLoader<T> loader, T name) {
        String src = loader.getShaderSource(name);

        List<String> lines = parseSodiumShader(loader, src);

        return String.join("\n", lines);
    }

    public static <T> String parseSodiumShader(ShaderLoader<T> loader, T name, ShaderConstants constants) {
        String src = loader.getShaderSource(name);

        List<String> lines = parseSodiumShader(loader, src);
        lines.addAll(1, constants.getDefineStrings());

        return String.join("\n", lines);
    }

    public static List<String> parseSodiumShader(ShaderLoader<?> loader, String src) {
        List<String> builder = new LinkedList<>();
        String line;

        try (BufferedReader reader = new BufferedReader(new StringReader(src))) {
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#import")) {
                    builder.addAll(resolveImport(loader, line));
                } else {
                    builder.add(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader sources", e);
        }

        return builder;
    }

    private static final Pattern IMPORT_PATTERN = Pattern.compile("#import <(?<name>.*)>");

    private static List<String> resolveImport(ShaderLoader<?> loader, String line) {
        Matcher matcher = IMPORT_PATTERN.matcher(line);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Malformed import statement (expected format: " + IMPORT_PATTERN + ")");
        }

        String name = matcher.group("name");
        String source = loader.getShaderSource(name);

        return ShaderParser.parseSodiumShader(loader, source);
    }
}
