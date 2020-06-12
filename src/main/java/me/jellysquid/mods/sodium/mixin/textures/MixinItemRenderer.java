package me.jellysquid.mods.sodium.mixin.textures;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.texture.SpriteExtended;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {
    @Inject(method = "renderBakedItemQuads", at = @At("HEAD"))
    private void preRenderItem(MatrixStack matrixStack, VertexConsumer vertices, List<BakedQuad> quads, ItemStack stack, int int_1, int int_2, CallbackInfo ci) {
        for (BakedQuad quad : quads) {
            ((SpriteExtended) ((ModelQuadView) quad).getSprite()).markActive();
        }
    }
}
