package me.jellysquid.mods.sodium.render.shader.tokens;

import me.jellysquid.mods.sodium.render.shader.ShaderParseContext;
import me.jellysquid.mods.sodium.render.shader.ShaderParser;
import me.jellysquid.mods.sodium.render.shader.tokens.TokenType;

import java.util.Collection;

public abstract class AbstractToken {
    private final TokenType type;
    private final int lineNumber;

    public AbstractToken(int lineNumber, TokenType type) {
        this.type = type;
        this.lineNumber = lineNumber;
    }

    public abstract void emit(ShaderParseContext context, Collection<String> output);

    public TokenType getType() {
        return this.type;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }
}
