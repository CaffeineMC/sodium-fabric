package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import java.util.function.IntSupplier;

public record ShaderTexture(int target, IntSupplier texture, ShaderTextureParameters params) {

}
