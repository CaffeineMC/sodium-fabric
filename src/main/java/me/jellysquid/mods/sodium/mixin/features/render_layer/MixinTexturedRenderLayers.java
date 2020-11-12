package me.jellysquid.mods.sodium.mixin.features.render_layer;

import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.SignType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(TexturedRenderLayers.class)
public class MixinTexturedRenderLayers {
    @Shadow
    @Final
    public static Map<SignType, SpriteIdentifier> WOOD_TYPE_TEXTURES;

    // Instantiating a SpriteIdentifier every time a sign tries to grab a texture identifier causes a significant
    // performance impact as no RenderLayer will ever be cached for the sprite. Minecraft already maintains a
    // SignType -> SpriteIdentifier cache but for some reason doesn't use it.
    @Inject(method = "getSignTextureId", at = @At("HEAD"), cancellable = true)
    private static void preGetSignTextureId(SignType type, CallbackInfoReturnable<SpriteIdentifier> ci) {
        if (WOOD_TYPE_TEXTURES != null) {
            SpriteIdentifier sprite = WOOD_TYPE_TEXTURES.get(type);

            if (type != null) {
                ci.setReturnValue(sprite);
            }
        }
    }
}
