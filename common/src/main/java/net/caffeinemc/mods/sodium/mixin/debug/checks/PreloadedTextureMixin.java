package net.caffeinemc.mods.sodium.mixin.debug.checks;

import net.caffeinemc.mods.sodium.client.render.util.DeferredRenderTask;
import net.minecraft.client.renderer.texture.PreloadedTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.concurrent.Executor;

@Mixin(PreloadedTexture.class)
public class PreloadedTextureMixin {
    /**
     * @author JellySquid
     * @reason Redirect asynchronous render commands to our helper
     */
    @Overwrite
    private static Executor executor(Executor executor) {
        return DeferredRenderTask::schedule;
    }
}
