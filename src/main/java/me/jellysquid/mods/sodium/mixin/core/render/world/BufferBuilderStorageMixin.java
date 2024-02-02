package me.jellysquid.mods.sodium.mixin.core.render.world;

import me.jellysquid.mods.sodium.client.render.chunk.NonStoringBuilderPool;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderBuffers.class)
public class BufferBuilderStorageMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SectionBufferBuilderPool;allocate(I)Lnet/minecraft/client/renderer/SectionBufferBuilderPool;"))
    private SectionBufferBuilderPool sodium$doNotAllocateChunks(int i) {
        return new NonStoringBuilderPool();
    }
}
