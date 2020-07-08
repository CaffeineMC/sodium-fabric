package me.jellysquid.mods.sodium.mixin.pipeline;

import me.jellysquid.mods.sodium.client.model.consumer.QuadVertexConsumer;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.sink.FallbackQuadSink;
import me.jellysquid.mods.sodium.client.render.block.BlockRenderPipeline;
import me.jellysquid.mods.sodium.client.render.pipeline.GlobalRenderer;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.client.util.ColorARGB;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import me.jellysquid.mods.sodium.client.util.rand.XoRoShiRoRandom;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer {
    private final XoRoShiRoRandom random = new XoRoShiRoRandom();

    @Inject(method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLjava/util/Random;JI)Z", at = @At("HEAD"), cancellable = true)
    private void preRenderBlockInWorld(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrixStack, VertexConsumer consumer, boolean cull, Random rand, long seed, int int_1, CallbackInfoReturnable<Boolean> cir) {
        GlobalRenderer renderer = GlobalRenderer.getInstance(world);
        BlockRenderPipeline blockRenderer = renderer.getBlockRenderer();

        boolean ret = blockRenderer.renderModel(null, world, model, state, pos, new FallbackQuadSink(consumer, matrixStack), cull, rand, seed);

        cir.setReturnValue(ret);
    }

    /**
     * @reason Use optimized vertex writer intrinsics, avoid allocations
     * @author JellySquid
     */
    @Overwrite
    public void render(MatrixStack.Entry entry, VertexConsumer vertexConsumer, BlockState blockState, BakedModel bakedModel, float red, float green, float blue, int light, int overlay) {
        XoRoShiRoRandom random = this.random;
        QuadVertexConsumer quadConsumer = (QuadVertexConsumer) vertexConsumer;

        // Clamp color ranges
        red = MathHelper.clamp(red, 0.0F, 1.0F);
        green = MathHelper.clamp(green, 0.0F, 1.0F);
        blue = MathHelper.clamp(blue, 0.0F, 1.0F);

        int defaultColor = ColorARGB.pack(red, green, blue, 1.0F);

        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
            List<BakedQuad> quads = bakedModel.getQuads(blockState, direction, random.setSeedAndReturn(42L));

            if (!quads.isEmpty()) {
                renderQuad(entry, quadConsumer, defaultColor, quads, light, overlay);
            }
        }

        List<BakedQuad> quads = bakedModel.getQuads(blockState, null, random.setSeedAndReturn(42L));

        if (!quads.isEmpty()) {
            renderQuad(entry, quadConsumer, defaultColor, quads, light, overlay);
        }
    }

    private static void renderQuad(MatrixStack.Entry entry, QuadVertexConsumer vertices, int defaultColor, List<BakedQuad> list, int light, int overlay) {
        if (list.isEmpty()) {
            return;
        }

        for (BakedQuad bakedQuad : list) {

            int color = bakedQuad.hasColor() ? defaultColor : 0xFFFFFFFF;

            ModelQuadView quad = ((ModelQuadView) bakedQuad);

            for (int i = 0; i < 4; i++) {
                vertices.vertexQuad(entry, quad.getX(i), quad.getY(i), quad.getZ(i), color, quad.getTexU(i), quad.getTexV(i),
                        light, overlay, ModelQuadUtil.getFacingNormal(bakedQuad.getFace()));
            }

            SpriteUtil.markSpriteActive(quad.getSprite());
        }
    }
}
