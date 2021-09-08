package me.jellysquid.mods.sodium.render.shader.tokens;

public class TokenParseException extends Throwable {
    public TokenParseException(String message) {
        super(message);
    }

    private TokenParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public static class Wrapped extends TokenParseException {
        public Wrapped(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
