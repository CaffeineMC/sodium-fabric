package me.jellysquid.mods.sodium.mixin.features.gui.hooks.console;


import me.jellysquid.mods.sodium.client.gui.console.ConsoleHooks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    MinecraftClient client;

    @Shadow
    @Final
    private BufferBuilderStorage buffers;

    @Unique
    private static boolean HAS_RENDERED_OVERLAY_ONCE = false;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;draw()V", shift = At.Shift.AFTER))
    private void onRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        // Do not start updating the console overlay until the font renderer is ready
        // This prevents the console from using tofu boxes for everything during early startup
        if (MinecraftClient.getInstance().getOverlay() != null) {
            if (!HAS_RENDERED_OVERLAY_ONCE) {
                return;
            }
        }

        this.client.getProfiler()
                .push("sodium_console_overlay");

        DrawContext drawContext = new DrawContext(this.client, this.buffers.getEntityVertexConsumers());

        ConsoleHooks.render(drawContext, GLFW.glfwGetTime());

        drawContext.draw();

        this.client.getProfiler()
                .pop();

        HAS_RENDERED_OVERLAY_ONCE = true;
    }
}
