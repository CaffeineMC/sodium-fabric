package me.jellysquid.mods.sodium.mixin.features.block;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.vertex.formats.ModelVertex;
import me.jellysquid.mods.sodium.client.render.vertex.VertexBufferWriter;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer {
    private final Xoroshiro128PlusPlusRandom random = new Xoroshiro128PlusPlusRandom(42L);

    /**
     * @reason Use optimized vertex writer intrinsics, avoid allocations
     * @author JellySquid
     */
    @Overwrite
    public void render(MatrixStack.Entry entry, VertexConsumer vertexConsumer, BlockState blockState, BakedModel bakedModel, float red, float green, float blue, int light, int overlay) {
        Xoroshiro128PlusPlusRandom random = this.random;

        // Clamp color ranges
        red = MathHelper.clamp(red, 0.0F, 1.0F);
        green = MathHelper.clamp(green, 0.0F, 1.0F);
        blue = MathHelper.clamp(blue, 0.0F, 1.0F);

        int defaultColor = ColorABGR.pack(red, green, blue, 1.0F);

        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(42L);
            List<BakedQuad> quads = bakedModel.getQuads(blockState, direction, random);

            if (!quads.isEmpty()) {
                renderQuad(entry, vertexConsumer, defaultColor, quads, light, overlay);
            }
        }

        random.setSeed(42L);
        List<BakedQuad> quads = bakedModel.getQuads(blockState, null, random);

        if (!quads.isEmpty()) {
            renderQuad(entry, vertexConsumer, defaultColor, quads, light, overlay);
        }
    }

    private static void renderQuad(MatrixStack.Entry entry, VertexConsumer vertexConsumer, int defaultColor, List<BakedQuad> list, int light, int overlay) {
        if (list.isEmpty()) {
            return;
        }

        var writer = VertexBufferWriter.of(vertexConsumer);

        for (BakedQuad bakedQuad : list) {
            int color = bakedQuad.hasColor() ? defaultColor : 0xFFFFFFFF;

            ModelQuadView quad = ((ModelQuadView) bakedQuad);

            try (MemoryStack stack = VertexBufferWriter.STACK.push()) {
                long buffer = stack.nmalloc(ModelVertex.STRIDE * 4);
                long ptr = buffer;

                for (int i = 0; i < 4; i++) {
                    ModelVertex.write(ptr, entry, quad.getX(i), quad.getY(i), quad.getZ(i), color, quad.getTexU(i), quad.getTexV(i),
                            light, overlay, ModelQuadUtil.getFacingNormal(bakedQuad.getFace()));

                    ptr += ModelVertex.STRIDE;
                }

                writer.push(buffer, 4, ModelVertex.FORMAT);
            }


            SpriteUtil.markSpriteActive(quad.getSprite());
        }
    }
}
