package me.jellysquid.mods.sodium.render.shader.tokens;

public interface TokenFactory {
    AbstractToken create(int num, String line) throws TokenParseException;
}
