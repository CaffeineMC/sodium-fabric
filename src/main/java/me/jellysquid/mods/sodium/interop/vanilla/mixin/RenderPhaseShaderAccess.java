package me.jellysquid.mods.sodium.interop.vanilla.mixin;

import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderInstance;

public interface RenderPhaseShaderAccess {
    Supplier<ShaderInstance> getShader();
}
