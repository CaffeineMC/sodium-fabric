package me.jellysquid.mods.sodium.render.shader.tokens.types;

import me.jellysquid.mods.sodium.render.shader.ShaderParseContext;
import me.jellysquid.mods.sodium.render.shader.tokens.AbstractToken;
import me.jellysquid.mods.sodium.render.shader.tokens.TokenType;

import java.util.Collection;

public final class VersionToken extends AbstractToken {
    private final String line;

    public VersionToken(int lineNumber, String line) {
        super(lineNumber, TokenType.VERSION);

        this.line = line;
    }

    @Override
    public void emit(ShaderParseContext context, Collection<String> output) {
        output.add(this.line);
        output.addAll(context.getConstants().getDefineStrings());
    }
}
