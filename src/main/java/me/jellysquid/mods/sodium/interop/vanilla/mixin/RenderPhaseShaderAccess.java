package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import net.minecraft.client.render.Shader;

import java.util.function.Supplier;

public interface RenderPhaseShaderAccess {
    Supplier<Shader> getShader();
}
