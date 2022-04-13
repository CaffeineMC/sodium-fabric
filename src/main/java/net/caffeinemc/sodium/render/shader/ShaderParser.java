package net.caffeinemc.sodium.render.shader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShaderParser {
    public static <T> String parseShader(ShaderLoader<T> loader, T name, ShaderConstants constants) {
        String src = loader.getShaderSource(name);

        List<String> lines = parseShader(loader, src);
        lines.addAll(1, constants.getDefineStrings());

        return String.join("\n", lines);
    }

    public static List<String> parseShader(ShaderLoader<?> loader, String src) {
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

        return ShaderParser.parseShader(loader, source);
    }
}
