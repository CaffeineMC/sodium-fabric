package net.caffeinemc.mods.sodium.mixin.debug.checks;

import net.caffeinemc.mods.sodium.client.render.util.DeferredRenderTask;
import net.caffeinemc.mods.sodium.client.render.util.RenderAsserts;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Mixin(TextureManager.class)
public class TextureManagerMixin {
    /**
     * @author JellySquid
     * @reason Use our own asynchronous executor for render commands
     */
    @Overwrite
    private static void execute(Runnable runnable) {
        DeferredRenderTask.schedule(runnable);
    }

    @Redirect(method = "reload", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenAcceptAsync(Ljava/util/function/Consumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    public <T> CompletableFuture<?> redirectReloadTask(CompletableFuture<T> instance, Consumer<T> consumer, Executor executor) {
        return instance.thenAcceptAsync(consumer, TextureManagerMixin::execute);
    }

    @Redirect(method = {
            "bindForSetup"
    }, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;isOnRenderThread()Z"))
    private boolean validateCurrentThread$bindTexture() {
        return RenderAsserts.validateCurrentThread();
    }
}
