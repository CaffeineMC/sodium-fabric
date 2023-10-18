package me.jellysquid.mods.sodium.mixin.core.render.world;

import me.jellysquid.mods.sodium.client.render.chunk.NonStoringSectionPack;
import net.minecraft.class_8901;
import net.minecraft.client.render.BufferBuilderStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BufferBuilderStorage.class)
public class BufferBuilderStorageMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_8901;method_54643(I)Lnet/minecraft/class_8901;"))
    private class_8901 sodium$doNotAllocateChunks(int i) {
        return new NonStoringSectionPack();
    }
}
