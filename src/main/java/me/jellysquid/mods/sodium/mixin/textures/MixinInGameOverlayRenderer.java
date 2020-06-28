package me.jellysquid.mods.sodium.mixin.textures;

import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class MixinInGameOverlayRenderer {
    @Inject(method = "renderFireOverlay", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/TextureManager;bindTexture(Lnet/minecraft/util/Identifier;)V", shift = At.Shift.AFTER))
    private static void preFireOverlayRender(MinecraftClient client, MatrixStack stack, CallbackInfo ci) {
        SpriteUtil.markSpriteActive(ModelLoader.FIRE_1.getSprite());
    }
}
