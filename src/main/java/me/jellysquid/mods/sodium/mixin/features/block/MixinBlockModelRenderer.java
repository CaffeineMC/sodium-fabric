package me.jellysquid.mods.sodium.mixin.features.block;

import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.render.immediate.model.BakedModelEncoder;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import java.util.List;

@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer {

    private final Random random = new LocalRandom(42L);

    /**
     * @reason Use optimized vertex writer intrinsics, avoid allocations
     * @author JellySquid
     */
    @Overwrite
    public void render(MatrixStack.Entry entry, VertexConsumer vertexConsumer, BlockState blockState, BakedModel bakedModel, float red, float green, float blue, int light, int overlay) {
        var writer = VertexBufferWriter.of(vertexConsumer);
        Random random = this.random;
        // Clamp color ranges
        red = MathHelper.clamp(red, 0.0F, 1.0F);
        green = MathHelper.clamp(green, 0.0F, 1.0F);
        blue = MathHelper.clamp(blue, 0.0F, 1.0F);
        int defaultColor = ColorABGR.pack(red, green, blue, 1.0F);
        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(42L);
            List<BakedQuad> quads = bakedModel.getQuads(blockState, direction, random);
            if (!quads.isEmpty()) {
                renderQuads(entry, writer, defaultColor, quads, light, overlay);
            }
        }
        random.setSeed(42L);
        List<BakedQuad> quads = bakedModel.getQuads(blockState, null, random);
        if (!quads.isEmpty()) {
            renderQuads(entry, writer, defaultColor, quads, light, overlay);
        }
    }

    @Unique
    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static void renderQuads(MatrixStack.Entry matrices, VertexBufferWriter writer, int defaultColor, List<BakedQuad> quads, int light, int overlay) {
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad bakedQuad = quads.get(i);
            BakedQuadView quad = (BakedQuadView) bakedQuad;
            int color = quad.hasColor() ? defaultColor : 0xFFFFFFFF;
            BakedModelEncoder.writeQuadVertices(writer, matrices, quad, color, light, overlay);
            SpriteUtil.markSpriteActive(quad.getSprite());
        }
    }
}
