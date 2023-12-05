package me.jellysquid.mods.sodium.mixin.debug.checks;

import me.jellysquid.mods.sodium.client.render.util.DeferredRenderTask;
import net.minecraft.client.texture.AsyncTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.concurrent.Executor;

@Mixin(AsyncTexture.class)
public class AsyncTextureMixin {
    /**
     * @author JellySquid
     * @reason Redirect asynchronous render commands to our helper
     */
    @Overwrite
    private static Executor createRenderThreadExecutor(Executor executor) {
        return DeferredRenderTask::schedule;
    }
}
