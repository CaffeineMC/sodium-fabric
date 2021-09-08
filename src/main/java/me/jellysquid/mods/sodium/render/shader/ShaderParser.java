package me.jellysquid.mods.sodium.render.shader;

import com.google.common.base.Throwables;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.render.shader.tokens.*;
import me.jellysquid.mods.sodium.render.shader.tokens.types.ImportToken;
import me.jellysquid.mods.sodium.render.shader.tokens.types.SourceToken;
import me.jellysquid.mods.sodium.render.shader.tokens.types.VersionToken;
import me.jellysquid.mods.thingl.shader.ShaderConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class ShaderParser {
    private static final Object2ReferenceMap<String, TokenFactory> DIRECTIVES;

    static {
        var directives = new Object2ReferenceOpenHashMap<String, TokenFactory>();
        directives.put("#import", ImportToken::new);
        directives.put("#version", VersionToken::new);

        DIRECTIVES = directives;
    }

    public static String parseShader(ShaderLoader loader, String src, ShaderConstants constants) {
        ShaderSource result = parse(src);

        if (!result.success()) {
            throwParseError(result);
        }

        ListIterator<AbstractToken> it = result.getTokenIterator();

        if (!it.hasNext()) {
            throw new RuntimeException("Empty shader file");
        }

        List<String> strings = new ArrayList<>();
        ShaderParseContext context = new ShaderParseContext(loader, constants);

        while (it.hasNext()) {
            AbstractToken token = it.next();
            token.emit(context, strings);
        }

        return String.join("\n", strings);
    }

    private static void throwParseError(ShaderSource result) {
        Logger logger = LogManager.getLogger();

        for (ShaderParseError error : result.getErrors()) {
            logger.error("(line {}) Pre-processor error: {}", error.lineNumber(),
                    Throwables.getStackTraceAsString(error.throwable()));
        }

        throw new RuntimeException("Failed to apply pre-processing to shader, see logs for more details");
    }

    public static ShaderSource parse(String src) {
        List<AbstractToken> tokens = new ArrayList<>();
        List<ShaderParseError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(src))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                AbstractToken token = null;
                Throwable error = null;

                try {
                    token = parseToken(lineNumber, line);
                } catch (Throwable caughtException) {
                    error = caughtException;
                }

                if (error != null) {
                    errors.add(new ShaderParseError(lineNumber, error));
                } else {
                    tokens.add(token);
                }

                lineNumber++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader sources", e);
        }

        return new ShaderSource(tokens, errors);
    }

    private static AbstractToken parseToken(int num, String line) throws TokenParseException {
        if (line.startsWith("#")) {
            for (Map.Entry<String, TokenFactory> entry : Object2ReferenceMaps.fastIterable(DIRECTIVES)) {
                if (!line.startsWith(entry.getKey())) {
                    continue;
                }

                try {
                    return entry.getValue()
                            .create(num, line);
                } catch (Throwable t) {
                    throw new TokenParseException.Wrapped("Failed to parse " + entry.getKey() + " directive", t);
                }
            }
        }

        return new SourceToken(num, line);
    }
}
