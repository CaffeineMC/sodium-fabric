package me.jellysquid.mods.sodium.render.shader;

import me.jellysquid.mods.sodium.render.shader.tokens.AbstractToken;

import java.util.*;

public final class ShaderSource {
    private final List<AbstractToken> tokens;
    private final List<ShaderParseError> errors;

    public ShaderSource(List<AbstractToken> tokens,
                        List<ShaderParseError> errors) {
        this.tokens = Collections.unmodifiableList(tokens);
        this.errors = Collections.unmodifiableList(errors);
    }

    public boolean success() {
        return this.errors.isEmpty();
    }

    public ListIterator<AbstractToken> getTokenIterator() {
        return this.tokens.listIterator();
    }

    public Iterable<ShaderParseError> getErrors() {
        return this.errors;
    }
}
