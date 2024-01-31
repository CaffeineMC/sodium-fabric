package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin {
    /**
     * @reason Ensure sprites rendered through renderSmooth/renderFlat in immediate-mode are marked as active.
     * This doesn't affect vanilla to my knowledge, but mods can trigger it.
     * @author embeddedt
     */
    @Inject(method = "renderQuad", at = @At("HEAD"))
    private void preRenderQuad(BlockRenderView world, BlockState state, BlockPos pos, VertexConsumer vertexConsumer, MatrixStack.Entry matrixEntry, BakedQuad quad, float brightness0, float brightness1, float brightness2, float brightness3, int light0, int light1, int light2, int light3, int overlay, CallbackInfo ci) {
        SpriteUtil.markSpriteActive(quad.getSprite());
    }
}
