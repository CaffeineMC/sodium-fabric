package me.jellysquid.mods.sodium.mixin.pipeline;

import me.jellysquid.mods.sodium.client.model.quad.consumer.FallbackQuadSink;
import me.jellysquid.mods.sodium.client.render.block.BlockRenderPipeline;
import me.jellysquid.mods.sodium.client.render.pipeline.GlobalRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer {
    @Inject(method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLjava/util/Random;JI)Z", at = @At("HEAD"), cancellable = true)
    private void preRenderBlock(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrixStack, VertexConsumer consumer, boolean cull, Random rand, long seed, int int_1, CallbackInfoReturnable<Boolean> cir) {
        GlobalRenderer renderer = GlobalRenderer.getInstance(world);
        BlockRenderPipeline blockRenderer = renderer.getBlockRenderer();

        boolean ret = blockRenderer.renderModel(null, world, model, state, pos, new FallbackQuadSink(consumer, matrixStack), cull, rand, seed);

        cir.setReturnValue(ret);
    }
}
