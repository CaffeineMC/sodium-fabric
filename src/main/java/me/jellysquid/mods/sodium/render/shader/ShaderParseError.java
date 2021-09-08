package me.jellysquid.mods.sodium.render.shader;

public record ShaderParseError(int lineNumber, Throwable throwable) {
}
