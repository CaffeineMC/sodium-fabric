package me.jellysquid.mods.sodium.render.shader.tokens.types;

import me.jellysquid.mods.sodium.render.shader.ShaderParseContext;
import me.jellysquid.mods.sodium.render.shader.tokens.AbstractToken;
import me.jellysquid.mods.sodium.render.shader.tokens.TokenParseException;
import me.jellysquid.mods.sodium.render.shader.tokens.TokenType;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImportToken extends AbstractToken {
    private static final Pattern IMPORT_PATTERN = Pattern.compile("#import <(?<namespace>.*):(?<path>.*)>");

    private final Identifier id;

    public ImportToken(int lineNumber, String line) throws TokenParseException {
        super(lineNumber, TokenType.IMPORT);

        Matcher matcher = IMPORT_PATTERN.matcher(line);

        if (!matcher.matches()) {
            throw new TokenParseException("Malformed import statement (expected format: " + IMPORT_PATTERN + ")");
        }

        String namespace = matcher.group("namespace");
        String path = matcher.group("path");

        this.id = new Identifier(namespace, path);

    }

    @Override
    public void emit(ShaderParseContext context, Collection<String> output) {
        String[] lines = context.importShader(this.id)
                .split("\n");

        Collections.addAll(output, lines);
    }
}
