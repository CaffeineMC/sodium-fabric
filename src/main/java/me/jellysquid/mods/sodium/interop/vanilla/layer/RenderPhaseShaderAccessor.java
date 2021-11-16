package me.jellysquid.mods.sodium.interop.vanilla.layer;

import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.client.render.Shader;

public interface RenderPhaseShaderAccessor {
    Optional<Supplier<Shader>> getSupplier();
}
